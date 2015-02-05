package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.collect.Sets.newHashSet;

public class ReservationCacheImpl extends AbstractService implements ReservationCache {

    private static final Logger log = LoggerFactory.getLogger(ReservationCacheImpl.class);

    private final SchedulerService schedulerService;

    /**
     * Caches a mapping from secret reservation keys to Reservation instances.
     */
    private final Map<Set<SecretReservationKey>, Reservation> reservationsBySrk = Maps.newHashMap();

    /**
     * Caches a mapping from node URNs to Reservation instances.
     */
    private final Map<NodeUrn, List<CacheItem<Reservation>>> reservationsByNodeUrn = Maps.newHashMap();

    private final Lock reservationsCacheLock = new ReentrantLock();

    private ScheduledFuture<?> cacheCleanupSchedule;

    private final Runnable cleanUpCacheRunnable = new Runnable() {
        @Override
        public void run() {
            cleanUpCache();
        }
    };

    @Inject
    public ReservationCacheImpl(SchedulerServiceFactory schedulerServiceFactory) {
        this.schedulerService = schedulerServiceFactory.create(-1, "ReservationCache");
    }

    @Override
    protected void doStart() {
        try {
            schedulerService.startAndWait();
            cacheCleanupSchedule = schedulerService.scheduleAtFixedRate(cleanUpCacheRunnable, 1, 1, TimeUnit.MINUTES);
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        try {
            cacheCleanupSchedule.cancel(true);
            cacheCleanupSchedule = null;
            schedulerService.stopAndWait();
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    public Set<Reservation> getAll() {
        return newHashSet(reservationsBySrk.values());
    }

    @Override
    public Optional<Reservation> lookup(final Set<SecretReservationKey> srks) {
        Reservation reservation = reservationsBySrk.get(srks);
        if (reservation != null) {
            reservation.touch();
            return Optional.of(reservation);
        }
        return Optional.absent();
    }

    @Override
    public Optional<Reservation> lookup(final NodeUrn nodeUrn, final DateTime timestamp) {

        log.trace("ReservationCacheImpl.lookup({}, {})", nodeUrn, timestamp);

        synchronized (reservationsByNodeUrn) {

            final List<CacheItem<Reservation>> entry = reservationsByNodeUrn.get(nodeUrn);

            if (entry == null) {
                log.trace("ReservationManagerImpl.lookup() CACHE MISS");
                return Optional.absent();
            }

            for (CacheItem<Reservation> item : entry) {

                final Interval effectiveInterval;
                final Reservation reservation = item.get();
                final DateTime reservationStart = reservation.getInterval().getStart();
                final DateTime reservationCancellation = reservation.getCancelled();

                if (reservationCancellation != null) {
                    if (reservationCancellation.isBefore(reservationStart)) {
                        continue;
                    } else {
                        effectiveInterval = new Interval(reservationStart, reservationCancellation);
                    }
                } else {
                    effectiveInterval = reservation.getInterval();
                }

                if (effectiveInterval.contains(timestamp)) {
                    item.touch();
                    log.trace("ReservationManagerImpl.lookup() CACHE HIT");
                    return Optional.of(reservation);
                }
            }
        }
        return Optional.absent();
    }

    @Override
    public Reservation put(final Reservation reservation) {

        log.trace("ReservationManagerImpl.put({})", reservation);

        reservationsCacheLock.lock();
        try {

            reservationsBySrk.put(reservation.getSecretReservationKeys(), reservation);

            for (NodeUrn nodeUrn : reservation.getNodeUrns()) {
                List<CacheItem<Reservation>> entry = reservationsByNodeUrn.get(nodeUrn);
                if (entry == null) {
                    entry = Lists.newArrayList();
                    reservationsByNodeUrn.put(nodeUrn, entry);
                }
                entry.add(new CacheItem<Reservation>(reservation));
            }

        } finally {
            reservationsCacheLock.unlock();
        }

        return reservation;
    }

    private void cleanUpCache() {

        log.trace("ReservationManagerImpl.cleanUpCache() starting");

        int removedNodeCacheEntries = 0;

        reservationsCacheLock.lock();
        try {

            for (Iterator<Map.Entry<NodeUrn, List<CacheItem<Reservation>>>> cacheIterator =
                         reservationsByNodeUrn.entrySet().iterator(); cacheIterator.hasNext(); ) {

                final Map.Entry<NodeUrn, List<CacheItem<Reservation>>> entry = cacheIterator.next();

                for (Iterator<CacheItem<Reservation>> itemIterator = entry.getValue().iterator();
                     itemIterator.hasNext(); ) {
                    final CacheItem<Reservation> item = itemIterator.next();
                    if (item.isOutdated()) {
                        itemIterator.remove();
                        removedNodeCacheEntries++;
                    }
                }

                if (entry.getValue().isEmpty()) {
                    cacheIterator.remove();
                }
            }

            int removedReservationMapEntries = 0;
            for (Iterator<Map.Entry<Set<SecretReservationKey>, Reservation>> iterator =
                         reservationsBySrk.entrySet().iterator(); iterator.hasNext(); ) {
                final Map.Entry<Set<SecretReservationKey>, Reservation> entry = iterator.next();

                if (entry.getValue().isOutdated()) {
                    iterator.remove();
                    removedReservationMapEntries++;
                }
            }

            log.trace("ReservationManagerImpl.cleanUpCache() removed {} node cache entries AND {} reservation map entries",
                    removedNodeCacheEntries, removedReservationMapEntries
            );

        } finally {
            reservationsCacheLock.unlock();
        }
    }

    @Override
    public void clear() {
        reservationsCacheLock.lock();
        try {
            reservationsByNodeUrn.clear();
            reservationsBySrk.clear();
        } finally {
            reservationsCacheLock.unlock();
        }
    }

    @Override
    public void remove(Reservation reservation) {
        reservationsCacheLock.lock();
        try {


            int removedNodeCacheEntries = 0;

            for (Iterator<Map.Entry<NodeUrn, List<CacheItem<Reservation>>>> cacheIterator =
                         reservationsByNodeUrn.entrySet().iterator(); cacheIterator.hasNext(); ) {

                final Map.Entry<NodeUrn, List<CacheItem<Reservation>>> entry = cacheIterator.next();

                for (Iterator<CacheItem<Reservation>> itemIt = entry.getValue().iterator(); itemIt.hasNext(); ) {

                    final CacheItem<Reservation> item = itemIt.next();
                    final Reservation res = item.get();

                    if (res == reservation) {
                        itemIt.remove();
                        removedNodeCacheEntries++;
                    }
                }

                if (entry.getValue().isEmpty()) {
                    cacheIterator.remove();
                }
            }

            log.trace("ReservationCacheImpl removed {} node cache entries",
                    removedNodeCacheEntries
            );

            reservationsBySrk.remove(reservation.getSecretReservationKeys());

        } finally {
            reservationsCacheLock.unlock();
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