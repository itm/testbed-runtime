package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationCancelledEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationClosedEvent;
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
import static de.uniluebeck.itm.tr.common.ReservationHelper.serialize;
import static org.junit.Assert.*;
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
	private PortalEventBus portalEventBus;

	@Mock
	private ScheduledFuture scheduledFuture;

	@Mock
	private ReservationCache reservationCache;

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
				portalEventBus,
				reservationCache
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

	@Test
	public void testIfCacheIsCleanedWhenReservationStops() throws Exception {

		setUpReservation1();

		// verify that reservation is created when asking for it the first time
		assertNotNull(reservationManager.getReservation(KNOWN_SECRET_RESERVATION_KEY_SET_1));
		verify(rsPersistence).getReservation(eq(KNOWN_SECRET_RESERVATION_KEY_1));

		// verify that reservation is returned from cache when asking for it the second time
		assertNotNull(reservationManager.getReservation(KNOWN_SECRET_RESERVATION_KEY_SET_1));
		verify(rsPersistence).getReservation(eq(KNOWN_SECRET_RESERVATION_KEY_1));

		// trigger ending reservation and validate that it was removed from cache
		/*
		reservationManager.on(ReservationClosedEvent.newBuilder()
						.setSerializedKey(serialize(KNOWN_SECRET_RESERVATION_KEY_SET_1))
						.build()
		);*/

		// TODO update test
		fail("TODO update test");

		assertNotNull(reservationManager.getReservation(KNOWN_SECRET_RESERVATION_KEY_SET_1));
		verify(rsPersistence, times(2)).getReservation(eq(KNOWN_SECRET_RESERVATION_KEY_1));
	}

	@Test
	public void testIfNodeToReservationCacheIsCleanedIfReservationIsCancelledBeforeStart() throws Exception {

		// set up reservation and check if it is correctly returned before cancelling it
		setUpReservation1();

		final DateTime cancellationTimestamp = DateTime.now();
		final DateTime reservationStart = cancellationTimestamp.plusMinutes(1);
		when(reservation1.getInterval()).thenReturn(new Interval(reservationStart, RESERVATION_INTERVAL_1.getEnd()));
		final DateTime res1Start = reservation1.getInterval().getStart();
		when(rsPersistence.getReservation(RESERVATION_1_NODE_URN, res1Start))
				.thenReturn(Optional.of(RESERVATION_DATA_1));
		assertSame(reservation1, reservationManager.getReservation(RESERVATION_1_NODE_URN, res1Start).get());

		// cancel the reservation, inform RM by having him catch the corresponding event and check if it is not
		// returned from the cache afterwards
		when(rsPersistence.getReservation(RESERVATION_1_NODE_URN, reservationStart.plusSeconds(1)))
				.thenReturn(Optional.<ConfidentialReservationData>absent());
		when(reservation1.getCancelled()).thenReturn(cancellationTimestamp);
		/*
		reservationManager.on(ReservationCancelledEvent.newBuilder()
						.setSerializedKey(serialize(KNOWN_SECRET_RESERVATION_KEY_1))
						.setTimestamp(cancellationTimestamp.getMillis()).build()
		);
		*/

		// TODO update test
		fail("TODO update test");

		assertFalse(
				reservationManager.getReservation(RESERVATION_1_NODE_URN, reservationStart.plusSeconds(1)).isPresent()
		);
	}

	@Test
	public void testIfNodeToReservationCacheIsCleanedIfReservationIsCancelledDuringReservationInterval()
			throws Exception {

		// set up reservation and check if it is correctly returned before cancelling it
		setUpReservation1();

		final DateTime res1Start = reservation1.getInterval().getStart();
		when(rsPersistence.getReservation(RESERVATION_1_NODE_URN, res1Start))
				.thenReturn(Optional.of(RESERVATION_DATA_1));
		assertSame(reservation1, reservationManager.getReservation(RESERVATION_1_NODE_URN, res1Start).get());

		// cancel the reservation, inform RM by having him catch the corresponding event and check if it is not
		// returned from the cache afterwards
		final DateTime cancellationTimestamp = DateTime.now();
		when(rsPersistence.getReservation(RESERVATION_1_NODE_URN, cancellationTimestamp.plusSeconds(1)))
				.thenReturn(Optional.<ConfidentialReservationData>absent());
		when(reservation1.getCancelled()).thenReturn(cancellationTimestamp);

		/*
		reservationManager.on(ReservationCancelledEvent.newBuilder()
						.setSerializedKey(serialize(KNOWN_SECRET_RESERVATION_KEY_1))
						.setTimestamp(cancellationTimestamp.getMillis()).build()
		);
		*/

		// TODO update test
		fail("TODO update test");

		final Optional<Reservation> result =
				reservationManager.getReservation(RESERVATION_1_NODE_URN, cancellationTimestamp.plusSeconds(1));
		assertFalse(result.isPresent());
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
		when(reservation1.getSerializedKey()).thenReturn(serialize(KNOWN_SECRET_RESERVATION_KEY_1));
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
		when(reservation2.getSerializedKey()).thenReturn(serialize(KNOWN_SECRET_RESERVATION_KEY_2));
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
		when(reservation3.getSerializedKey()).thenReturn(serialize(KNOWN_SECRET_RESERVATION_KEY_3));
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
