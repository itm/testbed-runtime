package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.ReservationHelper;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationDeletedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationMadeEvent;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.deserialize;
import static org.joda.time.DateTime.now;

public class ReservationManagerImpl extends AbstractService implements ReservationManager {

    private static final Logger log = LoggerFactory.getLogger(ReservationManager.class);

    private static final TimeUnit MS = TimeUnit.MILLISECONDS;
    private final Provider<RS> rs;
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
    private ScheduledFuture<?> cacheCleanupSchedule;

    @Inject
    public ReservationManagerImpl(final CommonConfig commonConfig,
                                  final Provider<RS> rs,
                                  final Provider<RSPersistence> rsPersistence,
                                  final DeviceDBService deviceDBService,
                                  final ReservationFactory reservationFactory,
                                  final SchedulerServiceFactory schedulerServiceFactory,
                                  final PortalEventBus portalEventBus) {
        this.commonConfig = checkNotNull(commonConfig);
        this.rs = checkNotNull(rs);
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
            replayReservationMadeEvents();
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
            for (Reservation reservation : reservationMap.values()) {
                reservation.stopAndWait();
            }
            portalEventBus.unregister(this);
            schedulerService.stopAndWait();
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    /**
     * Replay all {@link de.uniluebeck.itm.tr.iwsn.messages.ReservationMadeEvent}s of reservations that are
     * currently active of in the future to drive all subscribers to the current state (cf. event-sourced
     * architecture).
     */
    private void replayReservationMadeEvents() {

        log.trace("ReservationManagerImpl.replayReservationMadeEvents()");

        try {
            for (ConfidentialReservationData crd : rsPersistence.get().getActiveAndFutureReservations()) {
                final String serializedKey = ReservationHelper.serialize(crd.getSecretReservationKey());
                log.trace("Replaying ReservationMadeEvent for {}", serializedKey);
                portalEventBus.post(ReservationMadeEvent.newBuilder().setSerializedKey(serializedKey).build());
            }
        } catch (RSFault_Exception e) {
            throw propagate(e);
        }
    }

    @Subscribe
    public void onReservationMadeEvent(final ReservationMadeEvent event) {
        log.trace("ReservationManagerImpl.onReservationMadeEvent({})", event.getSerializedKey());
        // getReservation will set up any internal state necessary
        getReservation(deserialize(event.getSerializedKey()));
    }

    @Subscribe
    public void onReservationDeletedEvent(final ReservationDeletedEvent event) {
        log.trace("ReservationManagerImpl.onReservationDeletedEvent({})", event.getSerializedKey());
        // TODO implement me
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
                return reservationMap.get(srkSet);
            }

            reservation = createAndInitReservation(srkSet);
        }

        for (NodeUrn nodeUrn : reservation.getNodeUrns()) {
            putInCache(nodeUrn, reservation);
        }

        return reservation;
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
                reservedNodes,
                new Interval(data.getFrom(), data.getTo())
        );

        scheduleStartStop(reservation);

        reservationMap.put(srkSet1, reservation);

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
    private void scheduleStartStop(final Reservation reservation) {

        log.trace("ReservationManagerImpl.scheduleStartStop({})", reservation.getSerializedKey());

        if (reservation.isRunning()) {
            throw new IllegalStateException("Reservation instance should not be running yet!");
        }

        if (reservation.getInterval().containsNow()) {

            log.trace("ReservationManagerImpl.scheduleStartStop() for currently running reservation");

            final Duration startAfter = Duration.ZERO;
            final Duration stopAfter = new Duration(now(), reservation.getInterval().getEnd());

            final ReservationStartCallable startCallable = new ReservationStartCallable(portalEventBus, reservation);
            final ReservationStopCallable stopCallable = new ReservationStopCallable(portalEventBus, reservation);

            schedulerService.schedule(startCallable, startAfter.getMillis(), MS);
            schedulerService.schedule(stopCallable, stopAfter.getMillis(), MS);

        } else if (reservation.getInterval().isAfterNow()) {

            log.trace("ReservationManagerImpl.scheduleStartStop() for future reservation");

            final Duration startAfter = new Duration(now(), reservation.getInterval().getStart());
            final Duration stopAfter = new Duration(now(), reservation.getInterval().getEnd());

            final ReservationStartCallable startCallable = new ReservationStartCallable(portalEventBus, reservation);
            final ReservationStopCallable stopCallable = new ReservationStopCallable(portalEventBus, reservation);

            schedulerService.schedule(startCallable, startAfter.getMillis(), MS);
            schedulerService.schedule(stopCallable, stopAfter.getMillis(), MS);
        }
    }

    @Override
    public Reservation getReservation(final String secretReservationKeysBase64)
            throws ReservationUnknownException {
        return getReservation(deserialize(secretReservationKeysBase64));
    }

    @Override
    public Optional<Reservation> getReservation(final NodeUrn nodeUrn, final DateTime timestamp) {

        try {

            Optional<Reservation> reservation = lookupInCache(nodeUrn, timestamp);

            if (reservation.isPresent()) {
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
            List<ConfidentialReservationData> reservationDataList = rsPersistence.get().getReservations(timestamp, timestamp, null, null);

            synchronized (reservationMap) {
                return initReservations(reservationDataList);
            }


        } catch (RSFault_Exception e) {
            throw propagate(e);
        }
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
}
