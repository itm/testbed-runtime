package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerCache;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBusFactory;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.DeliveryManagerFactory;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStore;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.wsn.WSN;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;

public class FederatedReservationImpl extends AbstractService implements FederatedReservation {

	private static final Logger log = LoggerFactory.getLogger(FederatedReservationImpl.class);

	private final ResponseTrackerCache responseTrackerCache;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final List<ConfidentialReservationData> confidentialReservationDataList;

	private final ReservationEventBus reservationEventBus;

	private final ImmutableSet<NodeUrnPrefix> nodeUrnPrefixes;

	private final ImmutableSet<NodeUrn> nodeUrns;

	private final Interval interval;

	private final ImmutableSet<Entry> entries;

	private final WSNFederatorService wsnFederatorService;

	private final FederatorController federatorController;

	private final FederatedReservationEventBusAdapter reservationEventBusAdapter;

	private final DeliveryManager deliveryManager;

	@Inject
	public FederatedReservationImpl(final ReservationEventBusFactory reservationEventBusFactory,
									final ResponseTrackerFactory responseTrackerFactory,
									final ResponseTrackerCache responseTrackerCache,
									final WSNFederatorServiceFactory serviceFactory,
									final FederatorControllerFactory controllerFactory,
									final FederatedReservationEventBusAdapterFactory reservationEventBusAdapterFactory,
									final DeliveryManagerFactory deliveryManagerFactory,
									@Assisted final List<ConfidentialReservationData> confidentialReservationDataList,
									@Assisted final FederatedEndpoints<WSN> endpoints) {

		this.responseTrackerFactory = responseTrackerFactory;
		this.responseTrackerCache = responseTrackerCache;
		this.confidentialReservationDataList = confidentialReservationDataList;

		this.nodeUrnPrefixes = extractNodeUrnPrefixes(confidentialReservationDataList);
		this.nodeUrns = extractNodeUrns(confidentialReservationDataList);
		this.interval = extractInterval(confidentialReservationDataList);
		this.entries = createEntries();

		this.deliveryManager = deliveryManagerFactory.create(this);
		this.wsnFederatorService = serviceFactory.create(this, deliveryManager, endpoints, nodeUrnPrefixes, nodeUrns);
		this.reservationEventBus = reservationEventBusFactory.create(this);
		this.reservationEventBusAdapter = reservationEventBusAdapterFactory.create(this);
		this.federatorController = controllerFactory.create(this, endpoints, nodeUrnPrefixes, nodeUrns);
	}

	@Override
	protected void doStart() {

		log.trace(
				"FederatedReservationImpl.doStart(deliveryManager={}, reservationEventBus={}, federatorController={}, wsnFederatorService={}, reservationEventBusAdapter={})",
				deliveryManager, reservationEventBus, federatorController, wsnFederatorService,
				reservationEventBusAdapter
		);

		try {

			deliveryManager.startAndWait();
			reservationEventBus.startAndWait();
			reservationEventBusAdapter.startAndWait();
			wsnFederatorService.startAndWait();
			federatorController.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace(
				"FederatedReservationImpl.doStop(deliveryManager={}, reservationEventBus={}, federatorController={}, wsnFederatorService={}, reservationEventBusAdapter={})",
				deliveryManager, reservationEventBus, federatorController, wsnFederatorService,
				reservationEventBusAdapter
		);

		try {

			federatorController.stopAndWait();
			reservationEventBusAdapter.stopAndWait();
			reservationEventBus.stopAndWait();
			wsnFederatorService.stopAndWait();
			deliveryManager.stopAndWait();

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public Set<Entry> getEntries() {
		return entries;
	}

	private ImmutableSet<Entry> createEntries() {

		final ImmutableSet.Builder<Entry> entries = ImmutableSet.builder();

		for (ConfidentialReservationData crd : confidentialReservationDataList) {

			final NodeUrnPrefix nodeUrnPrefix = crd.getSecretReservationKey().getUrnPrefix();
			final String username = crd.getUsername();
			final String key = crd.getSecretReservationKey().getKey();

			entries.add(new Entry(nodeUrnPrefix, username, key, nodeUrns, interval, reservationEventBus));
		}

		return entries.build();
	}

	@Override
	public Set<NodeUrnPrefix> getNodeUrnPrefixes() {
		return nodeUrnPrefixes;
	}

	@Override
	public Set<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}

	@Override
	public ReservationEventBus getEventBus() {
		return reservationEventBus;
	}

	@Override
	public Interval getInterval() {
		return interval;
	}

	@Nullable
	@Override
	public DateTime getCancelled() {
		throw new UnsupportedOperationException("getCancelled() not yet implemented");
	}

	@Nullable
	@Override
	public DateTime getFinalized() {
		throw new UnsupportedOperationException("getFinalized() not yet implemented");
	}

	@Override
	public boolean isFinalized() {
		throw new UnsupportedOperationException("isFinalized() not yet implemented");
	}

	@Override
	public String getSerializedKey() {
		return serialize(getSecretReservationKeys());
	}

	@Override
	public Set<SecretReservationKey> getSecretReservationKeys() {
		final ImmutableSet.Builder<SecretReservationKey> keys = ImmutableSet.builder();
		for (ConfidentialReservationData crd : confidentialReservationDataList) {
			keys.add(crd.getSecretReservationKey());
		}
		return keys.build();
	}

	@Override
	public Set<ConfidentialReservationData> getConfidentialReservationData() {
		return newHashSet(confidentialReservationDataList);
	}

	@Override
	public ResponseTracker createResponseTracker(final Request request) {
		final ResponseTracker responseTracker = responseTrackerFactory.create(request, reservationEventBus);
		responseTrackerCache.put(request.getRequestId(), responseTracker);
		return responseTracker;
	}

	@Override
	public ResponseTracker getResponseTracker(final long requestId) {
		return responseTrackerCache.getIfPresent(requestId);
	}

	@Override
	public void enableVirtualization() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public void disableVirtualization() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public boolean isVirtualizationEnabled() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public ReservationEventStore getEventStore() {
		throw new RuntimeException("Not yet implemented!");
	}

	private Interval extractInterval(final List<ConfidentialReservationData> reservationDataList) {

		DateTime earliestEnd = null;
		DateTime latestStart = null;

		for (ConfidentialReservationData reservationData : reservationDataList) {

			if (earliestEnd == null || reservationData.getTo().isBefore(earliestEnd)) {
				earliestEnd = reservationData.getTo();
			}

			if (latestStart == null || reservationData.getFrom().isAfter(latestStart)) {
				latestStart = reservationData.getFrom();
			}
		}

		assert earliestEnd != null;
		assert latestStart != null;

		if (earliestEnd.isBefore(latestStart)) {
			throw new RuntimeException("It seems the end of the federated reservation is before the start.");
		}

		return new Interval(latestStart, earliestEnd);
	}

	private ImmutableSet<NodeUrn> extractNodeUrns(final List<ConfidentialReservationData> reservationData) {
		final ImmutableSet.Builder<NodeUrn> nodeUrns = ImmutableSet.builder();
		for (ConfidentialReservationData reservation : reservationData) {
			nodeUrns.addAll(reservation.getNodeUrns());
		}
		return nodeUrns.build();
	}

	private ImmutableSet<NodeUrnPrefix> extractNodeUrnPrefixes(
			final List<ConfidentialReservationData> reservationData) {
		final ImmutableSet.Builder<NodeUrnPrefix> nodeUrnPrefixes = ImmutableSet.builder();
		for (ConfidentialReservationData reservation : reservationData) {
			for (NodeUrn nodeUrn : reservation.getNodeUrns()) {
				nodeUrnPrefixes.add(nodeUrn.getPrefix());
			}
		}
		return nodeUrnPrefixes.build();
	}

	@Override
	public WSNFederatorService getWsnFederatorService() {
		return wsnFederatorService;
	}

	@Override
	public FederatorController getFederatorController() {
		return federatorController;
	}

	@Override
	public DeliveryManager getDeliveryManager() {
		return deliveryManager;
	}

	@Override
	public FederatedReservationEventBusAdapter getFederatedReservationEventBusAdapter() {
		return reservationEventBusAdapter;
	}
}
