package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerCache;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;

public class ReservationImpl extends AbstractService implements Reservation {

	private static final Logger log = LoggerFactory.getLogger(Reservation.class);

	private final PortalEventBus portalEventBus;

	private final Set<NodeUrn> nodeUrns;

	private final ReservationEventBus reservationEventBus;

	private final Interval interval;

	private final String username;

	private final String key;

	private final ResponseTrackerCache responseTrackerCache;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final CommonConfig commonConfig;

	private final Set<ConfidentialReservationData> confidentialReservationData;

	@Inject
	public ReservationImpl(final CommonConfig commonConfig,
						   final ReservationEventBusFactory reservationEventBusFactory,
						   final PortalEventBus portalEventBus,
						   final ResponseTrackerCache responseTrackerCache,
						   final ResponseTrackerFactory responseTrackerFactory,
						   @Assisted final List<ConfidentialReservationData> confidentialReservationDataList,
						   @Assisted("secretReservationKey") final String key,
						   @Assisted("username") final String username,
						   @Assisted final Set<NodeUrn> nodeUrns,
						   @Assisted final Interval interval) {
		this.commonConfig = checkNotNull(commonConfig);
		this.responseTrackerCache = checkNotNull(responseTrackerCache);
		this.responseTrackerFactory = checkNotNull(responseTrackerFactory);
		this.confidentialReservationData = newHashSet(checkNotNull(confidentialReservationDataList));
		this.key = checkNotNull(key);
		this.username = checkNotNull(username);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.nodeUrns = checkNotNull(nodeUrns);
		this.interval = checkNotNull(interval);
		this.reservationEventBus = checkNotNull(reservationEventBusFactory.create(this));
	}

	@Override
	protected void doStart() {
		log.trace("ReservationImpl.doStart()");
		try {
			reservationEventBus.startAndWait();
			notifyStarted();
			final ReservationStartedEvent event = ReservationStartedEvent
					.newBuilder()
					.setSerializedKey(getSerializedKey())
					.build();
			portalEventBus.post(event);
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("ReservationImpl.doStop()");
		try {
			final ReservationEndedEvent event = ReservationEndedEvent
					.newBuilder()
					.setSerializedKey(getSerializedKey())
					.build();
			portalEventBus.post(event);
			reservationEventBus.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public Set<Entry> getEntries() {
		return newHashSet(new Entry(
				commonConfig.getUrnPrefix(),
				username,
				key,
				nodeUrns,
				interval,
				reservationEventBus
		)
		);
	}

	@Override
	public Set<NodeUrnPrefix> getNodeUrnPrefixes() {
		return newHashSet(commonConfig.getUrnPrefix());
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
		try {
			return serialize(getSecretReservationKey());
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	private SecretReservationKey getSecretReservationKey() {
		return new SecretReservationKey().withKey(key).withUrnPrefix(commonConfig.getUrnPrefix());
	}

	@Override
	public Set<SecretReservationKey> getSecretReservationKeys() {
		return newHashSet(getSecretReservationKey());
	}

	@Override
	public Set<ConfidentialReservationData> getConfidentialReservationData() {
		return confidentialReservationData;
	}

	@Override
	public ResponseTracker createResponseTracker(final Request request) {
		if (responseTrackerCache.getIfPresent(request.getRequestId()) != null) {
			throw new IllegalArgumentException(
					"ResponseTracker for requestId \"" + request.getRequestId() + "\" already exists!"
			);
		}
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
		reservationEventBus.enableVirtualization();
	}

	@Override
	public void disableVirtualization() {
		reservationEventBus.disableVirtualization();
	}

	@Override
	public boolean isVirtualizationEnabled() {
		return reservationEventBus.isVirtualizationEnabled();
	}

	@Override
	public boolean equals(final Object o) {

		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final ReservationImpl that = (ReservationImpl) o;
		return interval.equals(that.interval) && nodeUrns.equals(that.nodeUrns);
	}

	@Override
	public int hashCode() {
		int result = nodeUrns.hashCode();
		result = 31 * result + interval.hashCode();
		return result;
	}
}
