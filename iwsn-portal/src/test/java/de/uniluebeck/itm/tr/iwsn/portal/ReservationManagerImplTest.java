package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Optional.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReservationManagerImplTest extends ReservationTestBase {

    static {
        Logging.setLoggingDefaults();
    }

    @Mock
    private Provider<RSPersistence> rsPersistenceProvider;

    @Mock
    private RSPersistence rsPersistence;

    @Mock
    private DeviceDBService deviceDBService;

    @Mock
    private ReservationFactory reservationFactory;

    @Mock
    private Reservation reservation1;

    @Mock
    private Reservation reservation2;

    @Mock
    private Reservation reservation3;

    @Mock
    private SchedulerServiceFactory schedulerServiceFactory;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private CommonConfig commonConfig;

    @Mock
    private ScheduledFuture scheduledFuture;

    @Mock
    private ReservationCache cache;

    private ReservationManagerImpl reservationManager;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {

        when(commonConfig.getUrnPrefix()).thenReturn(NODE_URN_PREFIX);
        when(schedulerServiceFactory.create(anyInt(), anyString())).thenReturn(schedulerService);
        when(rsPersistenceProvider.get()).thenReturn(rsPersistence);
        when(schedulerService
                        .scheduleAtFixedRate(Matchers.<Runnable>any(), anyLong(), anyLong(), Matchers.<TimeUnit>any())
        ).thenReturn(scheduledFuture);

        reservationManager = new ReservationManagerImpl(
                commonConfig,
                rsPersistenceProvider,
                reservationFactory,
                schedulerServiceFactory,
                cache
        );
    }

    @After
    public void tearDown() throws Exception {
        reservationManager.stopAndWait();
    }

    @Test
    public void testIfNonFinalizedReservationsAreStartedUponStartup() throws Exception {

        setUpReservation1();
        setUpReservation2();

        when(rsPersistence.getNonFinalizedReservations()).thenReturn(newArrayList(RESERVATION_1_DATA, RESERVATION_2_DATA));

        reservationManager.startAndWait();

        verify(reservation1).startAndWait();
        verify(reservation2).startAndWait();
    }

    @Test
    public void testIfReservationIsInstantiatedStartedAndCachedWhenCreatedInDB() throws Exception {

        setUpReservation1();

        reservationManager.rsPersistenceListener.onReservationMade(newArrayList(RESERVATION_1_DATA));

        //noinspection unchecked
        verify(reservationFactory).create(
                anyListOf(ConfidentialReservationData.class),
                anyString(),
                anyString(),
                isNull(DateTime.class),
                isNull(DateTime.class),
                any(SchedulerService.class),
                eq(RESERVATION_1_NODE_URN_SET),
                eq(RESERVATION_1_INTERVAL)
        );
        verify(reservation1).startAndWait();
        verify(cache).put(eq(reservation1));
    }

    @Test
    public void testIfReservationIsReturnedOnCacheHitForSrkSet() throws Exception {
        when(cache.lookup(RESERVATION_1_SRK_SET)).thenReturn(of(reservation1));
        assertSame(reservation1, reservationManager.getReservation(RESERVATION_1_SRK_SET));
    }

    @Test
    public void testIfReservationIsReturnedOnCacheHitForNodeUrnAndDateTime() throws Exception {
        when(cache.lookup(RESERVATION_1_NODE_URN, RESERVATION_1_INTERVAL.getStart())).thenReturn(of(reservation1));

        Reservation actual = reservationManager.getReservation(
                RESERVATION_1_NODE_URN,
                RESERVATION_1_INTERVAL.getStart()
        ).get();

        assertSame(reservation1, actual);
    }

    @Test
    public void testIfReservationIsInstantiatedStartedCachedAndReturnedOnCacheMiss() throws Exception {

        setUpReservation1();
        when(cache.lookup(RESERVATION_1_SRK_SET)).thenReturn(Optional.<Reservation>absent());

        Reservation actual = reservationManager.getReservation(RESERVATION_1_SRK_SET);

        //noinspection unchecked
        verify(reservationFactory).create(
                anyListOf(ConfidentialReservationData.class),
                anyString(),
                anyString(),
                isNull(DateTime.class),
                isNull(DateTime.class),
                any(SchedulerService.class),
                eq(RESERVATION_1_NODE_URN_SET),
                eq(RESERVATION_1_INTERVAL)
        );
        verify(reservation1).startAndWait();
        verify(cache).put(eq(reservation1));
        assertSame(reservation1, actual);
    }

    @Test
    public void testThatNoReservationIsCreatedIfReservationIsUnknown() throws Exception {

        setUpUnknownReservation();
        reservationManager.startAndWait();

        try {
            reservationManager.getReservation(UNKNOWN_SRK_SET);
        } catch (ReservationUnknownException e) {
            verifyZeroInteractions(deviceDBService);
            verifyZeroInteractions(reservationFactory);
        }
    }

    @Test
    public void testThatAllReservationsAreClosedOnShutdown() throws Exception {

        setUpReservation1();
        setUpReservation2();
        when(cache.lookup(eq(RESERVATION_1_SRK_SET))).thenReturn(Optional.of(reservation1));
        when(cache.lookup(eq(RESERVATION_2_SRK_SET))).thenReturn(Optional.of(reservation2));
        when(cache.getAll()).thenReturn(newHashSet(reservation1, reservation2));

        reservationManager.startAndWait();

        reservationManager.getReservation(RESERVATION_1_SRK_SET);
        reservationManager.getReservation(RESERVATION_2_SRK_SET);

        reservationManager.stopAndWait();

        verify(reservation1).stopAndWait();
        verify(reservation2).stopAndWait();
    }

    @Test
    public void testIfCacheIsStartedOnStartup() throws Exception {
        reservationManager.startAndWait();
        verify(cache).startAndWait();
    }

    @Test
    public void testIfCacheIsStoppedOnShutdown() throws Exception {
        reservationManager.startAndWait();
        reservationManager.stopAndWait();
        verify(cache).stopAndWait();
    }

    private void setUpReservation1() throws RSFault_Exception, UnknownSecretReservationKeyFault {
        when(rsPersistence.getReservation(RESERVATION_1_SRK)).thenReturn(RESERVATION_1_DATA);
        when(reservationFactory.create(
                        anyListOf(ConfidentialReservationData.class),
                        eq(RESERVATION_1_SRK.getKey()),
                        eq(USERNAME),
                        any(DateTime.class),
                        any(DateTime.class),
                        any(SchedulerService.class),
                        eq(RESERVATION_1_NODE_URN_SET),
                        eq(RESERVATION_1_INTERVAL)
                )
        ).thenReturn(reservation1);
        when(reservation1.getInterval()).thenReturn(RESERVATION_1_INTERVAL);
        when(reservation1.getSerializedKey()).thenReturn(serialize(RESERVATION_1_SRK));
    }

    private void setUpReservation2() throws RSFault_Exception, UnknownSecretReservationKeyFault {
        when(rsPersistence.getReservation(RESERVATION_2_SRK)).thenReturn(RESERVATION_2_DATA);
        when(reservationFactory.create(
                        anyListOf(ConfidentialReservationData.class),
                        eq(RESERVATION_2_SRK.getKey()),
                        eq(USERNAME),
                        any(DateTime.class),
                        any(DateTime.class),
                        any(SchedulerService.class),
                        eq(RESERVATION_2_NODE_URN_SET),
                        eq(RESERVATION_2_INTERVAL)
                )
        ).thenReturn(reservation2);
        when(reservation2.getInterval()).thenReturn(RESERVATION_2_INTERVAL);
        when(reservation2.getSerializedKey()).thenReturn(serialize(RESERVATION_2_SRK));
    }

    private void setUpReservation3() throws RSFault_Exception, UnknownSecretReservationKeyFault {
        when(rsPersistence.getReservation(RESERVATION_3_SRK)).thenReturn(RESERVATION_3_DATA);
        when(reservationFactory.create(
                        anyListOf(ConfidentialReservationData.class),
                        eq(RESERVATION_3_SRK.getKey()),
                        eq(USERNAME),
                        any(DateTime.class),
                        any(DateTime.class),
                        any(SchedulerService.class),
                        eq(RESERVATION_3_NODE_URN_SET),
                        eq(RESERVATION_3_INTERVAL)
                )
        ).thenReturn(reservation3);
        when(reservation3.getInterval()).thenReturn(RESERVATION_3_INTERVAL);
        when(reservation3.getSerializedKey()).thenReturn(serialize(RESERVATION_3_SRK));
    }

    private void setUpUnknownReservation() throws RSFault_Exception, UnknownSecretReservationKeyFault {
        when(cache.lookup(eq(UNKNOWN_SRK_SET))).thenReturn(Optional.<Reservation>absent());
        when(rsPersistence.getReservation(eq(UNKNOWN_SRK))).thenThrow(
                new UnknownSecretReservationKeyFault(
                        "not found",
                        new eu.wisebed.api.v3.common.UnknownSecretReservationKeyFault()
                )
        );
    }
}
