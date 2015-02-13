package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.inject.Provider;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReservationCacheImplTest extends ReservationTestBase {

    private final Provider<Stopwatch> stopwatchProvider = new Provider<Stopwatch>() {
        @Override
        public Stopwatch get() {
            return Stopwatch.createStarted(ticker);
        }
    };

    @Mock
    private SchedulerServiceFactory schedulerServiceFactory;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private Ticker ticker;

    @Mock
    private Reservation reservation1;

    @Mock
    private Reservation reservation2;

    @Mock
    private Reservation reservation3;

    @Mock
    private ScheduledFuture scheduledFuture;

    private ReservationCacheImpl cache;

    @Before
    public void setUp() throws Exception {

        when(ticker.read()).thenReturn(0L); // all test start at 0 ns

        when(schedulerServiceFactory.create(anyInt(), anyString())).thenReturn(schedulerService);
        //noinspection unchecked
        when(schedulerService.scheduleAtFixedRate(
                any(Runnable.class),
                anyInt(),
                anyInt(),
                any(TimeUnit.class)
        )).thenReturn(scheduledFuture);
        when(schedulerService.startAsync()).thenReturn(schedulerService);
        when(schedulerService.stopAsync()).thenReturn(schedulerService);

        when(reservation1.getCancelled()).thenReturn(null);
        when(reservation1.getFinalized()).thenReturn(null);
        when(reservation1.getNodeUrnPrefixes()).thenReturn(newHashSet(RESERVATION_1_NODE_URN.getPrefix()));
        when(reservation1.getSecretReservationKeys()).thenReturn(RESERVATION_1_SRK_SET);
        when(reservation1.getNodeUrns()).thenReturn(RESERVATION_1_NODE_URN_SET);
        when(reservation1.getConfidentialReservationData()).thenReturn(RESERVATION_1_DATA_SET);
        when(reservation1.getInterval()).thenReturn(RESERVATION_1_INTERVAL);
        when(reservation1.getSerializedKey()).thenReturn(serialize(RESERVATION_1_SRK));

        when(reservation2.getCancelled()).thenReturn(null);
        when(reservation2.getFinalized()).thenReturn(null);
        when(reservation2.getNodeUrnPrefixes()).thenReturn(newHashSet(RESERVATION_2_NODE_URN.getPrefix()));
        when(reservation2.getSecretReservationKeys()).thenReturn(RESERVATION_2_SRK_SET);
        when(reservation2.getNodeUrns()).thenReturn(RESERVATION_2_NODE_URN_SET);
        when(reservation2.getConfidentialReservationData()).thenReturn(RESERVATION_2_DATA_SET);
        when(reservation2.getInterval()).thenReturn(RESERVATION_2_INTERVAL);
        when(reservation2.getSerializedKey()).thenReturn(serialize(RESERVATION_2_SRK));

        when(reservation3.getCancelled()).thenReturn(null);
        when(reservation3.getFinalized()).thenReturn(null);
        when(reservation3.getNodeUrnPrefixes()).thenReturn(newHashSet(RESERVATION_3_NODE_URN.getPrefix()));
        when(reservation3.getSecretReservationKeys()).thenReturn(RESERVATION_3_SRK_SET);
        when(reservation3.getNodeUrns()).thenReturn(RESERVATION_3_NODE_URN_SET);
        when(reservation3.getConfidentialReservationData()).thenReturn(RESERVATION_3_DATA_SET);
        when(reservation3.getInterval()).thenReturn(RESERVATION_3_INTERVAL);
        when(reservation3.getSerializedKey()).thenReturn(serialize(RESERVATION_3_SRK));

        cache = new ReservationCacheImpl(schedulerServiceFactory, stopwatchProvider);
    }

    @After
    public void tearDown() throws Exception {
        if (cache.isRunning()) {
            cache.stopAsync().awaitTerminated();
        }
    }

    @Test
    public void testLookupByKeyForKnownReservation() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        assertSame(reservation1, cache.lookup(RESERVATION_1_SRK_SET).get());

        cache.put(reservation2);
        assertSame(reservation2, cache.lookup(RESERVATION_2_SRK_SET).get());

        cache.put(reservation3);
        assertSame(reservation3, cache.lookup(RESERVATION_3_SRK_SET).get());
    }

    @Test
    public void testLookupByKeyForUnknownReservation() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        assertFalse(cache.lookup(UNKNOWN_SRK_SET).isPresent());

        cache.put(reservation2);
        assertFalse(cache.lookup(UNKNOWN_SRK_SET).isPresent());

        cache.put(reservation3);
        assertFalse(cache.lookup(UNKNOWN_SRK_SET).isPresent());
    }

    @Test
    public void testLookupByNodeUrnsAndTimestampForKnownReservation() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        assertSame(reservation1, cache.lookup(RESERVATION_1_NODE_URN, RESERVATION_1_INTERVAL.getStart()).get());

        cache.put(reservation2);
        assertSame(reservation2, cache.lookup(RESERVATION_2_NODE_URN, RESERVATION_2_INTERVAL.getStart()).get());

        cache.put(reservation3);
        assertSame(reservation3, cache.lookup(RESERVATION_3_NODE_URN, RESERVATION_3_INTERVAL.getStart()).get());
    }

    @Test
    public void testLookupByNodeUrnAndTimestampForUnknownReservation() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        assertFalse(cache.lookup(UNKNOWN_NODE_URN, reservation1.getInterval().getStart()).isPresent());
        assertFalse(cache.lookup(UNKNOWN_NODE_URN, reservation2.getInterval().getStart()).isPresent());
        assertFalse(cache.lookup(UNKNOWN_NODE_URN, reservation3.getInterval().getStart()).isPresent());

        assertFalse(cache.lookup(UNKNOWN_NODE_URN, reservation1.getInterval().getStart().plusMinutes(5)).isPresent());
        assertFalse(cache.lookup(UNKNOWN_NODE_URN, reservation2.getInterval().getStart().plusMinutes(5)).isPresent());
        assertFalse(cache.lookup(UNKNOWN_NODE_URN, reservation3.getInterval().getStart().plusMinutes(5)).isPresent());

    }

    @Test
    public void testIfLookupFailsAfterRemovingReservation() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        cache.remove(reservation1);
        cache.remove(reservation2);
        cache.remove(reservation3);

        assertFalse(cache.lookup(RESERVATION_1_SRK_SET).isPresent());
        assertFalse(cache.lookup(RESERVATION_2_SRK_SET).isPresent());
        assertFalse(cache.lookup(RESERVATION_3_SRK_SET).isPresent());
    }

    @Test
    public void testIfLookupStillWorksForOtherAfterRemovingOneReservation() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        cache.remove(reservation1);

        assertFalse(cache.lookup(RESERVATION_1_SRK_SET).isPresent());
        assertTrue(cache.lookup(RESERVATION_2_SRK_SET).isPresent());
        assertTrue(cache.lookup(RESERVATION_3_SRK_SET).isPresent());
    }

    @Test
    public void testIfLookupFailsAfterClearingCache() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        cache.clear();

        assertFalse(cache.lookup(RESERVATION_1_SRK_SET).isPresent());
        assertFalse(cache.lookup(RESERVATION_2_SRK_SET).isPresent());
        assertFalse(cache.lookup(RESERVATION_3_SRK_SET).isPresent());
    }

    @Test
    public void testIfAllCachedReservationsAreReturnedByGetAll() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        Set<Reservation> all = cache.getAll();

        assertEquals(newHashSet(reservation1, reservation2, reservation3), all);
    }

    @Test
    public void testIfInterfaceMethodsCheckLifecycleState() throws Exception {
        try {
            cache.getAll();
            fail("Should have thrown IllegalStateException because it was not started yet.");
        } catch (IllegalStateException expected) {
            // ...
        }
        try {
            cache.lookup(RESERVATION_1_SRK_SET);
            fail("Should have thrown IllegalStateException because it was not started yet.");
        } catch (IllegalStateException expected) {
            // ...
        }
        try {
            cache.lookup(RESERVATION_1_NODE_URN, RESERVATION_1_INTERVAL.getStart());
            fail("Should have thrown IllegalStateException because it was not started yet.");
        } catch (IllegalStateException expected) {
            // ...
        }
        try {
            cache.put(reservation1);
            fail("Should have thrown IllegalStateException because it was not started yet.");
        } catch (IllegalStateException expected) {
            // ...
        }
        try {
            cache.clear();
            fail("Should have thrown IllegalStateException because it was not started yet.");
        } catch (IllegalStateException expected) {
            // ...
        }
        try {
            cache.remove(reservation1);
            fail("Should have thrown IllegalStateException because it was not started yet.");
        } catch (IllegalStateException expected) {
            // ...
        }
    }

    @Test
    public void testIfEntriesAreNotReturnedWhenOutdated() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        // +2 because ticker starts at 0 (zero) and +1 would result in exactly meeting the cache duration
        when(ticker.read()).thenReturn(TimeUnit.NANOSECONDS.convert(ReservationCacheImpl.CACHING_DURATION_MS + 2, TimeUnit.MILLISECONDS));

        assertTrue(cache.getAll().isEmpty());

        assertFalse(cache.lookup(RESERVATION_1_NODE_URN, reservation1.getInterval().getStart()).isPresent());
        assertFalse(cache.lookup(RESERVATION_2_NODE_URN, reservation2.getInterval().getStart()).isPresent());
        assertFalse(cache.lookup(RESERVATION_3_NODE_URN, reservation3.getInterval().getStart()).isPresent());

        assertFalse(cache.lookup(RESERVATION_1_SRK_SET).isPresent());
        assertFalse(cache.lookup(RESERVATION_2_SRK_SET).isPresent());
        assertFalse(cache.lookup(RESERVATION_3_SRK_SET).isPresent());
    }

    @Test
    public void testIfCleanUpEvictsEntriesWhenOutdated() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        // +2 because ticker starts at 0 (zero) and +1 would result in exactly meeting the cache duration
        when(ticker.read()).thenReturn(TimeUnit.NANOSECONDS.convert(ReservationCacheImpl.CACHING_DURATION_MS + 2, TimeUnit.MILLISECONDS));
        Set<Reservation> removed = cache.cleanUpCache();

        assertEquals(newHashSet(reservation1, reservation2, reservation3), removed);
    }

    @Test
    public void testThatCleanUpDoesNotRemoveRecentlyTouchedEntries() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        // -1 so as to touch items shortly before they time out
        when(ticker.read()).thenReturn(TimeUnit.NANOSECONDS.convert(ReservationCacheImpl.CACHING_DURATION_MS -1, TimeUnit.MILLISECONDS));
        cache.lookup(RESERVATION_1_NODE_URN, RESERVATION_1_INTERVAL.getStart());

        // +2 because ticker starts at 0 (zero) and +1 would result in exactly meeting the cache duration
        when(ticker.read()).thenReturn(TimeUnit.NANOSECONDS.convert(ReservationCacheImpl.CACHING_DURATION_MS + 2, TimeUnit.MILLISECONDS));
        Set<Reservation> removed = cache.cleanUpCache();

        assertEquals(newHashSet(reservation2, reservation3), removed);
    }

    @Test
    public void testIfAccessingCachedItemsRefreshesValidity() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        // -1 ms so entries should not be evicted yet
        when(ticker.read()).thenReturn(TimeUnit.NANOSECONDS.convert(ReservationCacheImpl.CACHING_DURATION_MS - 1, TimeUnit.MILLISECONDS));

        // validate it hasn't been evicted yet
        assertEquals(newHashSet(reservation1, reservation2, reservation3), cache.getAll());

        assertSame(reservation1, cache.lookup(RESERVATION_1_NODE_URN, reservation1.getInterval().getStart()).get());
        assertSame(reservation2, cache.lookup(RESERVATION_2_NODE_URN, reservation2.getInterval().getStart()).get());
        assertSame(reservation3, cache.lookup(RESERVATION_3_NODE_URN, reservation3.getInterval().getStart()).get());

        assertSame(reservation1, cache.lookup(RESERVATION_1_SRK_SET).get());
        assertSame(reservation2, cache.lookup(RESERVATION_2_SRK_SET).get());
        assertSame(reservation3, cache.lookup(RESERVATION_3_SRK_SET).get());

        // +2 because ticker starts at 0 (zero) and +1 would result in exactly meeting the cache duration
        when(ticker.read()).thenReturn(TimeUnit.NANOSECONDS.convert(ReservationCacheImpl.CACHING_DURATION_MS - 1, TimeUnit.MILLISECONDS));

        // validate that items have not been evicted because the reading access should have reset last access timestamp
        assertEquals(newHashSet(reservation1, reservation2, reservation3), cache.getAll());

        assertSame(reservation1, cache.lookup(RESERVATION_1_NODE_URN, reservation1.getInterval().getStart()).get());
        assertSame(reservation2, cache.lookup(RESERVATION_2_NODE_URN, reservation2.getInterval().getStart()).get());
        assertSame(reservation3, cache.lookup(RESERVATION_3_NODE_URN, reservation3.getInterval().getStart()).get());

        assertSame(reservation1, cache.lookup(RESERVATION_1_SRK_SET).get());
        assertSame(reservation2, cache.lookup(RESERVATION_2_SRK_SET).get());
        assertSame(reservation3, cache.lookup(RESERVATION_3_SRK_SET).get());
    }

    @Test
    public void testIfOtherEntriesStayInCacheWhenOneGetsEvicted() throws Exception {

        cache.startAsync().awaitRunning();

        cache.put(reservation1);
        cache.put(reservation2);
        cache.put(reservation3);

        // -1 ms so entries should not be evicted yet
        when(ticker.read()).thenReturn(TimeUnit.NANOSECONDS.convert(ReservationCacheImpl.CACHING_DURATION_MS - 1, TimeUnit.MILLISECONDS));

        // "touch" reservation2 and reservation3 so only reservation1 will be evicted
        assertSame(reservation2, cache.lookup(RESERVATION_2_NODE_URN, reservation2.getInterval().getStart()).get());
        assertSame(reservation3, cache.lookup(RESERVATION_3_NODE_URN, reservation3.getInterval().getStart()).get());

        assertSame(reservation2, cache.lookup(RESERVATION_2_SRK_SET).get());
        assertSame(reservation3, cache.lookup(RESERVATION_3_SRK_SET).get());

        // +2 because ticker starts at 0 (zero) and +1 would result in exactly meeting the cache duration
        when(ticker.read()).thenReturn(TimeUnit.NANOSECONDS.convert(ReservationCacheImpl.CACHING_DURATION_MS + 2, TimeUnit.MILLISECONDS));

        assertFalse(cache.lookup(RESERVATION_1_SRK_SET).isPresent());
        assertFalse(cache.lookup(RESERVATION_1_NODE_URN, reservation1.getInterval().getStart()).isPresent());
    }

}
