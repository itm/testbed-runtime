package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReservationManagerImplTest {

    static {
        Logging.setLoggingDefaults();
    }

    private static final String USERNAME = "My Awesome Username";

    private static final NodeUrnPrefix NODE_URN_PREFIX = new NodeUrnPrefix("urn:unit-test:");
    private static final Set<NodeUrn> RESERVATION_NODE_URNS_1 = newHashSet(new NodeUrn(NODE_URN_PREFIX + "0x0001"));
    private static final Set<NodeUrn> RESERVATION_NODE_URNS_2 = newHashSet(new NodeUrn(NODE_URN_PREFIX + "0x0002"));
    private static final Set<NodeUrn> RESERVATION_NODE_URNS_3 = newHashSet(new NodeUrn(NODE_URN_PREFIX + "0x0003"));


    static {
        KNOWN_SECRET_RESERVATION_KEY_1 = new SecretReservationKey()
                .withKey("YOU_KNOWN_ME_ONE")
                .withUrnPrefix(NODE_URN_PREFIX);

        KNOWN_SECRET_RESERVATION_KEY_2 = new SecretReservationKey()
                .withKey("YOU_KNOWN_ME_TWO")
                .withUrnPrefix(NODE_URN_PREFIX);

        KNOWN_SECRET_RESERVATION_KEY_3 = new SecretReservationKey()
                .withKey("YOU_KNOWN_ME_THREE")
                .withUrnPrefix(NODE_URN_PREFIX);

        UNKNOWN_SECRET_RESERVATION_KEY_1 = new SecretReservationKey()
                .withKey("YOU_KNOWN_ME_THREE")
                .withUrnPrefix(NODE_URN_PREFIX);
    }

    private static final Interval RESERVATION_INTERVAL_1 = new Interval(
            DateTime.now(),
            DateTime.now().plusHours(1)
    );
    private static final Interval RESERVATION_INTERVAL_2 = new Interval(
            DateTime.now().plusMinutes(1),
            DateTime.now().plusHours(1)
    );
    private static final Interval RESERVATION_INTERVAL_3 = new Interval(
            DateTime.now().minusHours(1),
            DateTime.now().minusMinutes(1)
    );


    private static final SecretReservationKey KNOWN_SECRET_RESERVATION_KEY_1;
    private static final Set<SecretReservationKey> KNOWN_SECRET_RESERVATION_KEY_SET_1 =
            newHashSet(KNOWN_SECRET_RESERVATION_KEY_1);
    private static final ConfidentialReservationData RESERVATION_DATA_1 = new ConfidentialReservationData()
            .withFrom(RESERVATION_INTERVAL_1.getStart())
            .withTo(RESERVATION_INTERVAL_1.getEnd())
            .withNodeUrns(RESERVATION_NODE_URNS_1)
            .withUsername(USERNAME)
            .withSecretReservationKey(KNOWN_SECRET_RESERVATION_KEY_1);
    private static final SecretReservationKey KNOWN_SECRET_RESERVATION_KEY_2;
    private static final Set<SecretReservationKey> KNOWN_SECRET_RESERVATION_KEY_SET_2 =
            newHashSet(KNOWN_SECRET_RESERVATION_KEY_2);
    private static final ConfidentialReservationData RESERVATION_DATA_2 = new ConfidentialReservationData()
            .withFrom(RESERVATION_INTERVAL_2.getStart())
            .withTo(RESERVATION_INTERVAL_2.getEnd())
            .withNodeUrns(RESERVATION_NODE_URNS_2)
            .withUsername(USERNAME)
            .withSecretReservationKey(KNOWN_SECRET_RESERVATION_KEY_2);
    private static final SecretReservationKey KNOWN_SECRET_RESERVATION_KEY_3;
    private static final Set<SecretReservationKey> KNOWN_SECRET_RESERVATION_KEY_SET_3 =
            newHashSet(KNOWN_SECRET_RESERVATION_KEY_3);
    private static final ConfidentialReservationData RESERVATION_DATA_3 = new ConfidentialReservationData()
            .withFrom(RESERVATION_INTERVAL_3.getStart())
            .withTo(RESERVATION_INTERVAL_3.getEnd())
            .withNodeUrns(RESERVATION_NODE_URNS_3)
            .withUsername(USERNAME)
            .withSecretReservationKey(KNOWN_SECRET_RESERVATION_KEY_3);
    private static final SecretReservationKey UNKNOWN_SECRET_RESERVATION_KEY_1;


    private static final Set<SecretReservationKey> UNKNOWN_SECRET_RESERVATION_KEY_SET =
            newHashSet(UNKNOWN_SECRET_RESERVATION_KEY_1);

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
    private PortalEventBus portalEventBus;

    @Mock
    private ScheduledFuture scheduledFuture;

    private ReservationManagerImpl reservationManager;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {

        when(commonConfig.getUrnPrefix()).thenReturn(NODE_URN_PREFIX);
        when(schedulerServiceFactory.create(anyInt(), anyString())).thenReturn(schedulerService);
        when(rsPersistenceProvider.get()).thenReturn(rsPersistence);
        when(schedulerService.scheduleAtFixedRate(Matchers.<Runnable>any(), anyLong(), anyLong(), Matchers.<TimeUnit>any())).thenReturn(scheduledFuture);

        reservationManager = new ReservationManagerImpl(
                commonConfig,
                rsPersistenceProvider,
                deviceDBService,
                reservationFactory,
                schedulerServiceFactory,
                portalEventBus
        );

        reservationManager.startAndWait();
    }

    @After
    public void tearDown() throws Exception {
        reservationManager.stopAndWait();
    }

    @Test
    public void testThatNullIsReturnedAndNoReservationIsCreatedIfReservationIsUnknown() throws Exception {

        setUpUnknownReservation();

        try {
            reservationManager.getReservation(UNKNOWN_SECRET_RESERVATION_KEY_SET);
        } catch (ReservationUnknownException e) {
            verifyZeroInteractions(deviceDBService);
            verifyZeroInteractions(reservationFactory);
        }
    }

    @Test
    public void testThatAllRunningReservationsAreShutDownWhenReservationManagerIsShutDown() throws Exception {

        setUpReservation1();
        setUpReservation2();

        reservationManager.getReservation(KNOWN_SECRET_RESERVATION_KEY_SET_1);
        reservationManager.getReservation(KNOWN_SECRET_RESERVATION_KEY_SET_2);

        reservationManager.stopAndWait();

        verify(reservation1).stopAndWait();
        verify(reservation2).stopAndWait();
    }


    private void setUpReservation1() throws RSFault_Exception, UnknownSecretReservationKeyFault {
        when(rsPersistence.getReservation(KNOWN_SECRET_RESERVATION_KEY_1)).thenReturn(RESERVATION_DATA_1);
        when(reservationFactory.create(
                        anyListOf(ConfidentialReservationData.class),
                        eq(KNOWN_SECRET_RESERVATION_KEY_1.getKey()),
                        eq(USERNAME),
                        any(DateTime.class),
                        any(DateTime.class),
                        any(SchedulerService.class),
                        eq(RESERVATION_NODE_URNS_1),
                        eq(RESERVATION_INTERVAL_1)
                )
        ).thenReturn(reservation1);
        when(reservation1.getInterval()).thenReturn(RESERVATION_INTERVAL_1);
        when(reservation1.getSerializedKey()).thenReturn(KNOWN_SECRET_RESERVATION_KEY_1.getKey());
    }

    private void setUpReservation2() throws RSFault_Exception, UnknownSecretReservationKeyFault {
        when(rsPersistence.getReservation(KNOWN_SECRET_RESERVATION_KEY_2)).thenReturn(RESERVATION_DATA_2);
        when(reservationFactory.create(
                        anyListOf(ConfidentialReservationData.class),
                        eq(KNOWN_SECRET_RESERVATION_KEY_2.getKey()),
                        eq(USERNAME),
                        any(DateTime.class),
                        any(DateTime.class),
                        any(SchedulerService.class),
                        eq(RESERVATION_NODE_URNS_2),
                        eq(RESERVATION_INTERVAL_2)
                )
        ).thenReturn(reservation2);
        when(reservation2.getInterval()).thenReturn(RESERVATION_INTERVAL_2);
        when(reservation2.getSerializedKey()).thenReturn(KNOWN_SECRET_RESERVATION_KEY_2.getKey());
    }

    private void setUpReservation3() throws RSFault_Exception, UnknownSecretReservationKeyFault {
        when(rsPersistence.getReservation(KNOWN_SECRET_RESERVATION_KEY_3)).thenReturn(RESERVATION_DATA_3);
        when(reservationFactory.create(
                        anyListOf(ConfidentialReservationData.class),
                        eq(KNOWN_SECRET_RESERVATION_KEY_3.getKey()),
                        eq(USERNAME),
                        any(DateTime.class),
                        any(DateTime.class),
                        any(SchedulerService.class),
                        eq(RESERVATION_NODE_URNS_3),
                        eq(RESERVATION_INTERVAL_3)
                )
        ).thenReturn(reservation3);
        when(reservation3.getInterval()).thenReturn(RESERVATION_INTERVAL_3);
        when(reservation3.getSerializedKey()).thenReturn(KNOWN_SECRET_RESERVATION_KEY_3.getKey());
    }

    private void setUpUnknownReservation() throws RSFault_Exception, UnknownSecretReservationKeyFault {
        when(rsPersistence.getReservation(eq(UNKNOWN_SECRET_RESERVATION_KEY_1))).thenThrow(
                new UnknownSecretReservationKeyFault(
                        "not found",
                        new eu.wisebed.api.v3.common.UnknownSecretReservationKeyFault()
                )
        );
    }


}
