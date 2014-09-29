package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationCancelledEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationFinalizedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationMadeEvent;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistenceListener;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.deserialize;
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;
import static org.joda.time.DateTime.now;

public class ReservationManagerImpl extends AbstractService implements ReservationManager {

    private static final Logger log = LoggerFactory.getLogger(ReservationManager.class);

    private static final TimeUnit MS = TimeUnit.MILLISECONDS;
    @VisibleForTesting
    final RSPersistenceListener rsPersistenceListener = new RSPersistenceListener() {
        @Override
        public void onReservationMade(final List<ConfidentialReservationData> crd) {
            log.trace("ReservationManagerImpl.onReservationMade({})", crd);

            final String secretReservationKeysBase64 = serialize(crd);

            initReservation(crd);
        }

        @Override
        public void onReservationCancelled(final List<ConfidentialReservationData> crd) {
            log.trace("ReservationManagerImpl.onReservationCancelled({})", crd);
            final String secretReservationKeyBase64 = serialize(crd);
            final Reservation reservation = getReservation(secretReservationKeyBase64);
            scheduleFinalization(reservation);

            if (reservation.getCancelled() == null) {
                log.error("reservation.getCancelled() is null. This is an error in the onReservationCancelled callback");
                return;
            }
            final ReservationCancelledEvent cancelledEvent = ReservationCancelledEvent.newBuilder().setSerializedKey(secretReservationKeyBase64).setTimestamp(reservation.getCancelled().getMillis()).build();
            portalEventBus.post(cancelledEvent);
        }

        @Override
        public void onReservationFinalized(final List<ConfidentialReservationData> crd) {
            // nothing should be done here as this ReservationManager will call RSPersistence.finalize()
        }
    };
    private final ReservationFactory reservationFactory;
    private final SchedulerService schedulerService;
    private final CommonConfig commonConfig;
    private final Provider<RSPersistence> rsPersistence;
    private final PortalEventBus portalEventBus;
    /**
     * Caches a mapping from secret reservation keys to Reservation instances.
     */
    private final Map<Set<SecretReservationKey>, Reservation> reservationMap = newHashMap();
    /**
     * Caches a mapping from node URNs to Reservation instances.
     */
    private final Map<NodeUrn, List<CacheItem<Reservation>>> nodeUrnToReservationCache = newHashMap();
    private final Map<Reservation, ScheduledFuture<Void>> finalizationSchedules = newHashMap();
    private ScheduledFuture<?> cacheCleanupSchedule;

    @Inject
    public ReservationManagerImpl(final CommonConfig commonConfig,
                                  final Provider<RSPersistence> rsPersistence,
                                  final DeviceDBService deviceDBService,
                                  final ReservationFactory reservationFactory,
                                  final SchedulerServiceFactory schedulerServiceFactory,
                                  final PortalEventBus portalEventBus) {
        this.commonConfig = checkNotNull(commonConfig);
        this.rsPersistence = checkNotNull(rsPersistence);
        this.reservationFactory = checkNotNull(reservationFactory);
        this.schedulerService = schedulerServiceFactory.create(-1, "ReservationManager");
        this.portalEventBus = checkNotNull(portalEventBus);
    }

    @Override
    protected void doStart() {
        log.trace("ReservationManagerImpl.doStart()");
        try {
            portalEventBus.register(this);
            schedulerService.startAndWait();
            rebuildReservationStates();
            rsPersistence.get().addListener(rsPersistenceListener);
            notifyStarted();
            cacheCleanupSchedule = schedulerService.scheduleAtFixedRate(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                cleanUpCache();
                                                                            }
                                                                        }, 1, 1, TimeUnit.MINUTES
            );
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        log.trace("ReservationManagerImpl.doStop()");
        try {
            cacheCleanupSchedule.cancel(false);
            synchronized (reservationMap) {
                for (Reservation reservation : reservationMap.values()) {
                    reservation.stopAndWait();
                }
            }
            portalEventBus.unregister(this);
            schedulerService.stopAndWait();
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }


    @Subscribe
    public void on(ReservationEndedEvent reservationEndedEvent) {
        scheduleFinalization(getReservation(reservationEndedEvent.getSerializedKey()));
    }

    @Subscribe
    public void on(ReservationCancelledEvent reservationCancelledEvent) {
        scheduleFinalization(getReservation(reservationCancelledEvent.getSerializedKey()));

    }

    /**
     * Replay all {@link de.uniluebeck.itm.tr.iwsn.messages.ReservationMadeEvent}s of reservations that are
     * currently active of in the future to drive all subscribers to the current state (cf. event-sourced
     * architecture).
     */
    private void rebuildReservationStates() {

        log.trace("ReservationManagerImpl.rebuildReservationStates()");

        try {
            for (ConfidentialReservationData crd : rsPersistence.get().getNonFinalizedReservations()) {
                final String serializedKey = serialize(crd.getSecretReservationKey());
                log.trace("Rebuilding state for {}", serializedKey);


                // initReservation triggers scheduleLifecycleEvents (rebuilding events)
                initReservation(newArrayList(crd));
            }
        } catch (RSFault_Exception e) {
            throw propagate(e);
        }
    }

    @Override
    public Reservation getReservation(final Set<SecretReservationKey> srks)
            throws ReservationUnknownException {

        log.trace("ReservationManagerImpl.getReservation(secretReservationKey={})", srks);

        // filter out additional keys that are not for this testbed (= urn prefix) and make a set so we can match in map
        for (Iterator<SecretReservationKey> it = srks.iterator(); it.hasNext(); ) {
            SecretReservationKey current = it.next();
            if (!commonConfig.getUrnPrefix().equals(current.getUrnPrefix())) {
                it.remove();
            }
        }

        final Set<SecretReservationKey> srkSet = newHashSet(srks);

        final Reservation reservation;
        synchronized (reservationMap) {

            if (reservationMap.containsKey(srkSet)) {
                reservation = reservationMap.get(srkSet);
                touchFinalizationCache(reservation);
                return reservation;
            }

            reservation = createAndInitReservation(srkSet);
        }

        for (NodeUrn nodeUrn : reservation.getNodeUrns()) {
            putInCache(nodeUrn, reservation);
        }

        return reservation;
    }

    private void touchFinalizationCache(Reservation reservation) {
        synchronized (finalizationSchedules) {
            if (finalizationSchedules.containsKey(reservation)) {
                log.trace("ReservationManagerImpl.touchFinalizationCache({})", reservation);
                scheduleFinalization(reservation);
            }
        }
    }

    /**
     * Only call when synchronized on reservationMap. Creates a new reservation and puts a reference into the map.
     */
    private Reservation createAndInitReservation(final Set<SecretReservationKey> srkSet) {

        checkArgument(srkSet.size() == 1,
                "There must be exactly one secret reservation key as this is a single URN-prefix implementation."
        );

        final SecretReservationKey srk = srkSet.iterator().next();
        final ConfidentialReservationData crd;

        try {
            crd = rsPersistence.get().getReservation(srk);
        } catch (UnknownSecretReservationKeyFault f) {
            throw new ReservationUnknownException(srkSet, f);
        } catch (RSFault_Exception e) {
            throw propagate(e);
        }

        if (crd == null) {
            throw new ReservationUnknownException(srkSet);
        }

        return initReservation(newArrayList(crd));
    }

    /**
     * Only call when synchronized on reservationMap. Creates a new reservation and puts a reference into the map.
     */
    private Reservation initReservation(final List<ConfidentialReservationData> confidentialReservationDataList) {

        final ConfidentialReservationData data = confidentialReservationDataList.get(0);
        final Set<SecretReservationKey> srkSet1 = newHashSet(data.getSecretReservationKey());
        final Set<NodeUrn> reservedNodes = newHashSet(data.getNodeUrns());

        final Reservation reservation = reservationFactory.create(
                confidentialReservationDataList,
                data.getSecretReservationKey().getKey(),
                data.getUsername(),
                data.getCancelled(),
                data.getFinalized(),
                reservedNodes,
                new Interval(data.getFrom(), data.getTo())
        );

        if (!reservationMap.containsKey(srkSet1)) {
            scheduleLifecycleEvents(reservation);
            reservationMap.put(srkSet1, reservation);
        }


        return reservation;
    }

    private List<Reservation> initReservations(final List<ConfidentialReservationData> confidentialReservationDataList) {
        List<Reservation> reservations = new ArrayList<Reservation>(confidentialReservationDataList.size());
        for (ConfidentialReservationData data : confidentialReservationDataList) {
            reservations.add(initReservation(newArrayList(data)));
        }
        return reservations;
    }

    /**
     * Schedules starting and stopping the @link{Reservation} instance depending on its interval. If the reservation is
     * already running {@link com.google.common.util.concurrent.Service#startAndWait()} will be called immediately. The
     * callables scheduled will also trigger a post of a {@link de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent}.
     *
     * @param reservation the reservation
     */
    private void scheduleLifecycleEvents(final Reservation reservation) {

        log.trace("ReservationManagerImpl.scheduleLifecycleEvents({})", reservation.getSerializedKey());

        if (reservation.isRunning()) {
            throw new IllegalStateException("Reservation instance should not be running yet!");
        }

        if (reservation.isFinalized()) {
            reservation.startAndWait();
            scheduleFinalization(reservation);
            return;
        }

        portalEventBus.post(ReservationMadeEvent.newBuilder().setSerializedKey(reservation.getSerializedKey()).build());

        if (reservation.getCancelled() != null && reservation.getCancelled().isBefore(reservation.getInterval().getStart())) {
            // The reservation was cancelled before it was started
            portalEventBus.post(ReservationCancelledEvent.newBuilder().setSerializedKey(reservation.getSerializedKey()).setTimestamp(reservation.getCancelled().getMillis()).build());
            return;
        } else if (reservation.getInterval().getStart().isBeforeNow()) {
            final Duration startAfter = Duration.ZERO;
            final ReservationStartCallable startCallable = new ReservationStartCallable(portalEventBus, reservation);
            schedulerService.schedule(startCallable, startAfter.getMillis(), MS);
        } else {
            log.trace("ReservationManagerImpl.scheduleLifecycleEvents(): scheduling start for later");
            final Duration startAfter = new Duration(now(), reservation.getInterval().getStart());
            final ReservationStartCallable startCallable = new ReservationStartCallable(portalEventBus, reservation);
            schedulerService.schedule(startCallable, startAfter.getMillis(), MS);
        }

        if (reservation.isCancelled()) {
            portalEventBus.post(ReservationCancelledEvent.newBuilder().setSerializedKey(reservation.getSerializedKey()).build());
        } else if (reservation.getCancelled() != null && reservation.getInterval().contains(reservation.getCancelled())) {
            log.trace("ReservationManagerImpl.scheduleLifecycleEvents(): scheduling cancellation for later");
            final Duration cancellAfter = new Duration(now(), reservation.getCancelled());
            final ReservationCancellCallable cancellCallable = new ReservationCancellCallable(portalEventBus, reservation);
            schedulerService.schedule(cancellCallable, cancellAfter.getMillis(), MS);
        } else if (reservation.getInterval().isBeforeNow()) {
            log.trace("ReservationManagerImpl.scheduleLifecycleEvents(): scheduling stop for now");
            final Duration stopAfter = Duration.ZERO;
            final ReservationEndCallable stopCallable = new ReservationEndCallable(portalEventBus, reservation);
            schedulerService.schedule(stopCallable, stopAfter.getMillis(), MS);
        } else {
            log.trace("ReservationManagerImpl.scheduleLifecycleEvents(): scheduling stop for later");
            final Duration stopAfter = new Duration(now(), reservation.getInterval().getEnd());
            final ReservationEndCallable stopCallable = new ReservationEndCallable(portalEventBus, reservation);
            schedulerService.schedule(stopCallable, stopAfter.getMillis(), MS);
        }


    }

    private void scheduleFinalization(Reservation reservation) {
        synchronized (finalizationSchedules) {
            if (finalizationSchedules.containsKey(reservation)) {
                ScheduledFuture<Void> future = finalizationSchedules.remove(reservation);
                if (future != null) {
                    future.cancel(true);
                }
            }
            final ReservationFinalizeCallable callable = new ReservationFinalizeCallable(reservation);
            ScheduledFuture<Void> finalizationFuture;
            if (reservation.isRunning()) {
                finalizationFuture = schedulerService.schedule(callable, 5, TimeUnit.MINUTES);
            } else {
                finalizationFuture = schedulerService.schedule(callable, 0, TimeUnit.MILLISECONDS);
            }
            finalizationSchedules.put(reservation, finalizationFuture);
        }
    }

    @Override
    public Reservation getReservation(final String secretReservationKeysBase64)
            throws ReservationUnknownException {
        if (secretReservationKeysBase64 == null || secretReservationKeysBase64.length() == 0) {
            return null;
        }
        return getReservation(deserialize(secretReservationKeysBase64));
    }

    @Override
    public Optional<Reservation> getReservation(final NodeUrn nodeUrn, final DateTime timestamp) {

        try {

            Optional<Reservation> reservation = lookupInCache(nodeUrn, timestamp);

            if (reservation.isPresent()) {
                touchFinalizationCache(reservation.get());
                return reservation;
            }

            Optional<ConfidentialReservationData> reservationData =
                    rsPersistence.get().getReservation(nodeUrn, timestamp);

            if (!reservationData.isPresent()) {
                return Optional.absent();
            }

            synchronized (reservationMap) {
                reservation = Optional.of(initReservation(newArrayList(reservationData.get())));
                putInCache(nodeUrn, reservation.get());
                return reservation;
            }

        } catch (RSFault_Exception e) {
            throw propagate(e);
        }
    }

    @Override
    public List<Reservation> getReservations(DateTime timestamp) {
        try {

            final List<ConfidentialReservationData> reservationDataList =
                    rsPersistence.get().getReservations(timestamp, timestamp, null, null, null);

            synchronized (reservationMap) {
                return initReservations(reservationDataList);
            }


        } catch (RSFault_Exception e) {
            throw propagate(e);
        }
    }


    @Override
    public Collection<Reservation> getNonFinalizedReservations() {
        return reservationMap.values();
    }

    private Optional<Reservation> lookupInCache(final NodeUrn nodeUrn, final DateTime timestamp) {
        log.trace("ReservationManagerImpl.lookupInCache({}, {})", nodeUrn, timestamp);
        synchronized (nodeUrnToReservationCache) {
            final List<CacheItem<Reservation>> entry = nodeUrnToReservationCache.get(nodeUrn);
            if (entry == null) {
                log.trace("ReservationManagerImpl.lookupInCache() CACHE MISS");
                return Optional.absent();
            }
            for (CacheItem<Reservation> item : entry) {
                if (item.get().getInterval().contains(timestamp)) {
                    item.touch();
                    log.trace("ReservationManagerImpl.lookupInCache() CACHE HIT");
                    return Optional.of(item.get());
                }
            }
        }
        return Optional.absent();
    }

    private void putInCache(final NodeUrn nodeUrn, final Reservation reservation) {
        log.trace("ReservationManagerImpl.putInCache({}, {})", nodeUrn, reservation);
        synchronized (nodeUrnToReservationCache) {
            List<CacheItem<Reservation>> entry = nodeUrnToReservationCache.get(nodeUrn);
            if (entry == null) {
                entry = newArrayList();
                nodeUrnToReservationCache.put(nodeUrn, entry);
            }
            entry.add(new CacheItem<Reservation>(reservation));
        }
    }

    private void cleanUpCache() {
        log.trace("ReservationManagerImpl.cleanUpCache() starting");

        int removed = 0;

        synchronized (nodeUrnToReservationCache) {

            for (Iterator<Map.Entry<NodeUrn, List<CacheItem<Reservation>>>> cacheIterator =
                         nodeUrnToReservationCache.entrySet().iterator(); cacheIterator.hasNext(); ) {

                final Map.Entry<NodeUrn, List<CacheItem<Reservation>>> entry = cacheIterator.next();

                for (Iterator<CacheItem<Reservation>> itemIterator = entry.getValue().iterator();
                     itemIterator.hasNext(); ) {
                    final CacheItem<Reservation> item = itemIterator.next();
                    if (item.isOutdated()) {
                        itemIterator.remove();
                        removed++;
                    }
                }

                if (entry.getValue().isEmpty()) {
                    cacheIterator.remove();
                }
            }
        }

        log.trace("ReservationManagerImpl.cleanUpCache() removed {} entries", removed);
    }

    @VisibleForTesting
    void clearCache() {
        synchronized (nodeUrnToReservationCache) {
            nodeUrnToReservationCache.clear();
        }
    }

    private static class CacheItem<T> {

        private static final long CACHING_DURATION_MS = TimeUnit.MINUTES.toMillis(30);

        private final T obj;

        private long lastTouched;

        public CacheItem(final T obj) {
            this.obj = obj;
            this.lastTouched = System.currentTimeMillis();
        }

        public boolean isOutdated() {
            return System.currentTimeMillis() - lastTouched > CACHING_DURATION_MS;
        }

        public void touch() {
            lastTouched = System.currentTimeMillis();
        }

        public T get() {
            return obj;
        }

    }

    private class ReservationFinalizeCallable implements Callable<Void> {
        final Reservation reservation;

        public ReservationFinalizeCallable(final Reservation reservation) {
            this.reservation = reservation;
        }

        @Override
        public Void call() throws Exception {
            log.trace("ReservationFinalizeCallable.call({})", reservation);
            if (!reservation.isFinalized()) {
                Set<SecretReservationKey> srks;
                synchronized (finalizationSchedules) {
                    finalizationSchedules.remove(reservation);
                    srks = deserialize(reservation.getSerializedKey());
                    // Finalize reservations of this testbed, filter federated reservations
                    for (SecretReservationKey current : srks) {
                        if (commonConfig.getUrnPrefix().equals(current.getUrnPrefix())) {
                            rsPersistence.get().finalizeReservation(current);
                        }
                    }
                }
                synchronized (reservationMap) {
                    reservationMap.remove(srks);
                }
                final ReservationFinalizedEvent event = ReservationFinalizedEvent.newBuilder().setSerializedKey(reservation.getSerializedKey()).build();
                portalEventBus.post(event);
            }
            if (reservation.isRunning()) {
                reservation.stopAndWait();
            }


            return null;
        }
    }
}
