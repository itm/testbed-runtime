package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerCache;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationClosedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationOpenedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStore;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStoreFactory;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistenceListener;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;

public class ReservationImpl extends AbstractService implements Reservation {

	private static final Logger log = LoggerFactory.getLogger(Reservation.class);

	private final Set<NodeUrn> nodeUrns;

	private final ReservationEventBus reservationEventBus;

	private final Interval interval;

	private final String username;

	private final String key;

	private final ResponseTrackerCache responseTrackerCache;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final CommonConfig commonConfig;

	private final Set<ConfidentialReservationData> confidentialReservationData;

	private final ReservationEventStore reservationEventStore;

	private final RSPersistence rsPersistence;

	@Nullable
	private DateTime cancelled;

    @Nullable
    private DateTime finalized;

	private final RSPersistenceListener rsPersistenceListener = new RSPersistenceListener() {
		@Override
		public void onReservationMade(final List<ConfidentialReservationData> crd) {
			// nothing to do
		}

		@Override
		public void onReservationCancelled(final List<ConfidentialReservationData> crd) {
			if (crd.get(0).getSecretReservationKey().equals(ReservationImpl.this.getSecretReservationKey())) {
				ReservationImpl.this.cancelled = crd.get(0).getCancelled();
			}
		}

		@Override
		public void onReservationFinalized(
				final List<ConfidentialReservationData> crd) {
            if (crd.get(0).getSecretReservationKey().equals(ReservationImpl.this.getSecretReservationKey())) {
                ReservationImpl.this.finalized = crd.get(0).getFinalized();
            }
		}
	};

	@Inject
	public ReservationImpl(final CommonConfig commonConfig,
						   final RSPersistence rsPersistence,
						   final ReservationEventBusFactory reservationEventBusFactory,
						   final ResponseTrackerCache responseTrackerCache,
						   final ResponseTrackerFactory responseTrackerFactory,
						   final ReservationEventStoreFactory reservationEventStoreFactory,
						   @Assisted final List<ConfidentialReservationData> confidentialReservationDataList,
						   @Assisted("secretReservationKey") final String key,
                           @Assisted("username") final String username,
                           @Assisted("cancelled") final DateTime cancelled,
                           @Assisted("finalized") final DateTime finalized,
						   @Assisted final Set<NodeUrn> nodeUrns,
						   @Assisted final Interval interval) {
        this.rsPersistence = checkNotNull(rsPersistence);
		this.commonConfig = checkNotNull(commonConfig);
		this.responseTrackerCache = checkNotNull(responseTrackerCache);
		this.responseTrackerFactory = checkNotNull(responseTrackerFactory);
		this.confidentialReservationData = newHashSet(checkNotNull(confidentialReservationDataList));
		this.key = checkNotNull(key);
		this.username = checkNotNull(username);
        this.cancelled = cancelled;
        this.finalized = finalized;
		this.nodeUrns = checkNotNull(nodeUrns);
		this.interval = checkNotNull(interval);
		this.reservationEventBus = checkNotNull(reservationEventBusFactory.create(this));
		this.reservationEventStore = reservationEventStoreFactory.createOrLoad(this);
	}

	@Override
	protected void doStart() {
		log.trace("ReservationImpl.doStart()");
		try {
			reservationEventStore.startAndWait();
			reservationEventBus.startAndWait();
            reservationEventBus.post(ReservationOpenedEvent.newBuilder().setSerializedKey(getSerializedKey()).build());
			rsPersistence.addListener(rsPersistenceListener);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("ReservationImpl.doStop()");
		try {
            reservationEventBus.post(ReservationClosedEvent.newBuilder().setSerializedKey(getSerializedKey()).build());
			rsPersistence.removeListener(rsPersistenceListener);
			reservationEventBus.stopAndWait();
			reservationEventStore.stopAndWait();
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
	public ReservationEventBus getEventBus() {
		return reservationEventBus;
	}

	@Override
	public Interval getInterval() {
		return interval;
	}

	@Override
	@Nullable
	public DateTime getCancelled() {
		return cancelled;
	}

	@Nullable
	@Override
	public DateTime getFinalized() {
		return finalized;
	}

	@Override
	public boolean isFinalized() {
		return getFinalized() != null && getFinalized().isBeforeNow();
	}

    @Override
    public boolean isCancelled() {
        return  getCancelled() != null && getCancelled().isBeforeNow();
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
	public ReservationEventStore getEventStore() {
		return reservationEventStore;
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
