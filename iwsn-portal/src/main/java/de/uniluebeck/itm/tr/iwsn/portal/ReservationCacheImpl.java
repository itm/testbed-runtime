package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.collect.Sets.newHashSet;

public class ReservationCacheImpl extends AbstractService implements ReservationCache {

    private Provider<Stopwatch> stopwatchProvider;

    @VisibleForTesting
    protected static final long CACHING_DURATION_MS = TimeUnit.MINUTES.toMillis(30);

    protected static class CacheItem<T> {

        private final Stopwatch stopwatch;

        private final T obj;

        public CacheItem(final Stopwatch stopwatch, final T obj) {
            this.stopwatch = stopwatch;
            this.obj = obj;
        }

        public boolean isOutdated() {
            return stopwatch.elapsed(TimeUnit.MILLISECONDS) > CACHING_DURATION_MS;
        }

        public void touch() {
            stopwatch.reset();
        }

        public T get() {
            return obj;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(ReservationCacheImpl.class);

    /**
     * Caches a mapping from secret reservation keys to Reservation instances.
     */
    protected final Map<Set<SecretReservationKey>, CacheItem<Reservation>> reservationsBySrk = Maps.newHashMap();

    /**
     * Caches a mapping from node URNs to Reservation instances.
     */
    protected final Map<NodeUrn, List<CacheItem<Reservation>>> reservationsByNodeUrn = Maps.newHashMap();

    private final Runnable cleanUpCacheRunnable = new Runnable() {
        @Override
        public void run() {
            cleanUpCache();
        }
    };

    protected final Lock reservationsCacheLock = new ReentrantLock();

    protected final SchedulerService schedulerService;

    protected ScheduledFuture<?> cacheCleanupSchedule;

    @Inject
    public ReservationCacheImpl(final SchedulerServiceFactory schedulerServiceFactory,
                                final Provider<Stopwatch> stopwatchProvider) {
        this.schedulerService = schedulerServiceFactory.create(-1, "ReservationCache");
        this.stopwatchProvider = stopwatchProvider;
    }

    @Override
    protected void doStart() {
        log.trace("ReservationCacheImpl.doStart()");
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
        log.trace("ReservationCacheImpl.doStop()");
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
        log.trace("ReservationCacheImpl.getAll()");
        checkRunning();
        Set<Reservation> reservations = newHashSet();
        for (CacheItem<Reservation> item : reservationsBySrk.values()) {
            if (!item.isOutdated()) {
                reservations.add(item.get());
                item.touch();
            }
        }
        return reservations;
    }

    @Override
    public Optional<Reservation> lookup(final Set<SecretReservationKey> srks) {
        log.trace("ReservationCacheImpl.lookup({})", srks);
        checkRunning();
        CacheItem<Reservation> item = reservationsBySrk.get(srks);
        if (item != null) {
            if (item.isOutdated()) {
                return Optional.absent();
            }
            Reservation reservation = item.get();
            reservation.touch();
            return Optional.of(reservation);
        }
        return Optional.absent();
    }

    @Override
    public Optional<Reservation> lookup(final NodeUrn nodeUrn, final DateTime timestamp) {

        log.trace("ReservationCacheImpl.lookup({}, {})", nodeUrn, timestamp);
        checkRunning();

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
                    if (!item.isOutdated()) {
                        item.touch();
                        log.trace("ReservationManagerImpl.lookup() CACHE HIT");
                        return Optional.of(reservation);
                    }
                }
            }
        }
        return Optional.absent();
    }

    @Override
    public void put(final Reservation reservation) {

        log.trace("ReservationManagerImpl.put({})", reservation);
        checkRunning();

        reservationsCacheLock.lock();
        try {

            CacheItem<Reservation> item = new CacheItem<Reservation>(stopwatchProvider.get(), reservation);

            reservationsBySrk.put(reservation.getSecretReservationKeys(), item);

            for (NodeUrn nodeUrn : reservation.getNodeUrns()) {
                List<CacheItem<Reservation>> entry = reservationsByNodeUrn.get(nodeUrn);
                if (entry == null) {
                    entry = Lists.newArrayList();
                    reservationsByNodeUrn.put(nodeUrn, entry);
                }
                entry.add(item);
            }

        } finally {
            reservationsCacheLock.unlock();
        }
    }

    @Override
    public void clear() {
        checkRunning();
        log.trace("ReservationCacheImpl.clear()");
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
        log.trace("ReservationCacheImpl.remove({})", reservation);
        checkRunning();
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

    private void checkRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("ReservationCache is not running! Did you forget to call start()?");
        }
    }

    /**
     * Cleans up the cache. All outdated items will be removed from both maps.
     *
     * @return the set of reservations that was removed from the two caches
     */
    @VisibleForTesting
    protected Set<Reservation> cleanUpCache() {

        log.trace("ReservationManagerImpl.cleanUpCache()");

        final Set<Reservation> removed = newHashSet();

        reservationsCacheLock.lock();
        try {

            for (Iterator<Map.Entry<NodeUrn, List<CacheItem<Reservation>>>> cacheIterator =
                         reservationsByNodeUrn.entrySet().iterator(); cacheIterator.hasNext(); ) {

                final Map.Entry<NodeUrn, List<CacheItem<Reservation>>> entry = cacheIterator.next();

                for (Iterator<CacheItem<Reservation>> itemIterator = entry.getValue().iterator();
                     itemIterator.hasNext(); ) {
                    final CacheItem<Reservation> item = itemIterator.next();
                    if (item.isOutdated()) {
                        removed.add(item.get());
                        itemIterator.remove();
                    }
                }

                if (entry.getValue().isEmpty()) {
                    cacheIterator.remove();
                }
            }

            for (Iterator<Map.Entry<Set<SecretReservationKey>, CacheItem<Reservation>>> iterator =
                         reservationsBySrk.entrySet().iterator(); iterator.hasNext(); ) {

                final Map.Entry<Set<SecretReservationKey>, CacheItem<Reservation>> entry = iterator.next();
                final CacheItem<Reservation> item = entry.getValue();

                if (item.isOutdated()) {
                    iterator.remove();
                    removed.add(item.get());
                }
            }

            log.trace("ReservationManagerImpl.cleanUpCache() removed {} entries: {}", removed.size(), removed);

            return removed;

        } finally {
            reservationsCacheLock.unlock();
        }
    }
}