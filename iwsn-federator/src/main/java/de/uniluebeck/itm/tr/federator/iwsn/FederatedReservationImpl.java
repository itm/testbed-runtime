package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerCache;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBusFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.wsn.WSN;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.List;
import java.util.Set;

import static de.uniluebeck.itm.tr.iwsn.portal.ReservationHelper.serialize;

public class FederatedReservationImpl extends AbstractService implements FederatedReservation {

	private final ResponseTrackerCache responseTrackerCache;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final List<ConfidentialReservationData> confidentialReservationDataList;

	private final ReservationEventBus reservationEventBus;

	private final ImmutableSet<NodeUrnPrefix> nodeUrnPrefixes;

	private final ImmutableSet<NodeUrn> nodeUrns;

	private final Interval interval;

	private final ImmutableSet<Entry> entries;

	private final WSNFederatorService wsnFederatorService;

	private final WSNFederatorController wsnFederatorController;

	@Inject
	public FederatedReservationImpl(final ReservationEventBusFactory reservationEventBusFactory,
									final ResponseTrackerFactory responseTrackerFactory,
									final ResponseTrackerCache responseTrackerCache,
									final WSNFederatorServiceFactory serviceFactory,
									final WSNFederatorControllerFactory controllerFactory,
									@Assisted final List<ConfidentialReservationData> confidentialReservationDataList,
									@Assisted final FederatedEndpoints<WSN> endpoints) {

		this.responseTrackerFactory = responseTrackerFactory;
		this.responseTrackerCache = responseTrackerCache;
		this.confidentialReservationDataList = confidentialReservationDataList;

		this.nodeUrnPrefixes = extractNodeUrnPrefixes(confidentialReservationDataList);
		this.nodeUrns = extractNodeUrns(confidentialReservationDataList);
		this.interval = extractInterval(confidentialReservationDataList);
		this.entries = createEntries();

		this.wsnFederatorController = controllerFactory.create(endpoints, nodeUrnPrefixes, nodeUrns);
		this.wsnFederatorService = serviceFactory.create(wsnFederatorController, endpoints, nodeUrnPrefixes, nodeUrns);
		this.reservationEventBus = reservationEventBusFactory.create(this);
	}

	@Override
	protected void doStart() {
		try {
			reservationEventBus.startAndWait();
			wsnFederatorController.startAndWait();
			wsnFederatorService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			wsnFederatorService.stopAndWait();
			wsnFederatorController.stopAndWait();
			reservationEventBus.stopAndWait();
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
	public ReservationEventBus getReservationEventBus() {
		return reservationEventBus;
	}

	@Override
	public Interval getInterval() {
		return interval;
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

		if (earliestEnd.isAfter(latestStart)) {
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

	private ImmutableSet<NodeUrnPrefix> extractNodeUrnPrefixes(final List<ConfidentialReservationData> reservationData) {
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
	public WSNFederatorController getWsnFederatorController() {
		return wsnFederatorController;
	}
}
