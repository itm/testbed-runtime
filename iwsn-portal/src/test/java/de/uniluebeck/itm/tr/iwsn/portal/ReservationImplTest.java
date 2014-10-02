package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerCache;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStore;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStoreFactory;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReservationImplTest {

	private static final Set<NodeUrn> NODE_URNS = newHashSet(new NodeUrn("urn:unit-test:0x0001"));

	private static final Interval INTERVAL_FROM_NOW_TO_1HOUR = new Interval(DateTime.now(), DateTime.now().plusHours(1));
    private static final Interval INTERVAL_STARTING_IN_PAST_ENDING_IN_FUTURE = new Interval(DateTime.now().minusHours(1), DateTime.now().plusHours(1));
    private static final Interval INTERVAL_IN_THE_PAST = new Interval(DateTime.now().minusHours(4), DateTime.now().minusHours(2));
    private static final Interval INTERVAL_IN_FUTURE = new Interval(DateTime.now().plusHours(1), DateTime.now().plusHours(2));

	private static final String username = "Horst Ackerpella";

	@Mock
	private ReservationEventBusFactory reservationEventBusFactory;

	@Mock
	private ReservationEventBus reservationEventBus;

	@Mock
	private ResponseTrackerCache responseTrackerTimedCache;

	@Mock
	private ResponseTrackerFactory responseTrackerFactory;

	@Mock
	private CommonConfig commonConfig;

	@Mock
	private ConfidentialReservationData confidentialReservationData;

	@Mock
	private ReservationEventStoreFactory reservationEventStoreFactory;

	@Mock
	private ReservationEventStore reservationEventStore;

	@Mock
	private PortalEventBus portalEventBus;

	@Mock
	private RSPersistence rsPersistence;

    @Mock
    private SchedulerService schedulerService;

	private ReservationImpl reservation;

	@Before
	public void setUp() throws Exception {
		when(reservationEventBusFactory.create(Matchers.<Reservation>any())).thenReturn(reservationEventBus);
		when(reservationEventStoreFactory.createOrLoad(Matchers.<Reservation>any())).thenReturn(reservationEventStore);

	}

    private void setupReservationJustStarted() {
        reservation = new ReservationImpl(
                commonConfig,
                rsPersistence,
                reservationEventBusFactory,
                responseTrackerTimedCache,
                responseTrackerFactory,
                reservationEventStoreFactory,
                portalEventBus,
                schedulerService,
                newArrayList(confidentialReservationData),
                "someRandomReservationIdHere",
                username,
                null,
                null,
                NODE_URNS,
                INTERVAL_FROM_NOW_TO_1HOUR
        );
    }

    private void setupReservationOngoing() {
        reservation = new ReservationImpl(
                commonConfig,
                rsPersistence,
                reservationEventBusFactory,
                responseTrackerTimedCache,
                responseTrackerFactory,
                reservationEventStoreFactory,
                portalEventBus,
                schedulerService,
                newArrayList(confidentialReservationData),
                "someRandomReservationIdHere",
                username,
                null,
                null,
                NODE_URNS,
                INTERVAL_STARTING_IN_PAST_ENDING_IN_FUTURE
        );
    }
    private void setupReservationEndedAndFinalized() {
        reservation = new ReservationImpl(
                commonConfig,
                rsPersistence,
                reservationEventBusFactory,
                responseTrackerTimedCache,
                responseTrackerFactory,
                reservationEventStoreFactory,
                portalEventBus,
                schedulerService,
                newArrayList(confidentialReservationData),
                "someRandomReservationIdHere",
                username,
                null,
                DateTime.now().minusHours(1),
                NODE_URNS,
                INTERVAL_IN_THE_PAST
        );
    }

    private void setupReservationEnded() {
        reservation = new ReservationImpl(
                commonConfig,
                rsPersistence,
                reservationEventBusFactory,
                responseTrackerTimedCache,
                responseTrackerFactory,
                reservationEventStoreFactory,
                portalEventBus,
                schedulerService,
                newArrayList(confidentialReservationData),
                "someRandomReservationIdHere",
                username,
                null,
                null,
                NODE_URNS,
                INTERVAL_IN_THE_PAST
        );
    }

    private void setupReservationNotYetStarted() {
        reservation = new ReservationImpl(
                commonConfig,
                rsPersistence,
                reservationEventBusFactory,
                responseTrackerTimedCache,
                responseTrackerFactory,
                reservationEventStoreFactory,
                portalEventBus,
                schedulerService,
                newArrayList(confidentialReservationData),
                "someRandomReservationIdHere",
                username,
                null,
                null,
                NODE_URNS,
                INTERVAL_IN_FUTURE
        );
    }

    private void setupReservationCancelledBeforeStart() {
        reservation = new ReservationImpl(
                commonConfig,
                rsPersistence,
                reservationEventBusFactory,
                responseTrackerTimedCache,
                responseTrackerFactory,
                reservationEventStoreFactory,
                portalEventBus,
                schedulerService,
                newArrayList(confidentialReservationData),
                "someRandomReservationIdHere",
                username,
                DateTime.now(),
                null,
                NODE_URNS,
                INTERVAL_IN_FUTURE
        );
    }

    private void setupReservationCancelledAfterStart() {
        reservation = new ReservationImpl(
                commonConfig,
                rsPersistence,
                reservationEventBusFactory,
                responseTrackerTimedCache,
                responseTrackerFactory,
                reservationEventStoreFactory,
                portalEventBus,
                schedulerService,
                newArrayList(confidentialReservationData),
                "someRandomReservationIdHere",
                username,
                DateTime.now().minusHours(3),
                null,
                NODE_URNS,
                INTERVAL_IN_THE_PAST
        );
    }

	@Test
	public void testThatReservationEventBusIsCreatedAndStartedWhenStartingReservation() throws Exception {
        setupReservationJustStarted();
		reservation.startAndWait();
		verify(reservationEventBus).startAndWait();
	}

	@Test
	public void testThatReservationEventBusIsStoppedWhenStoppingReservation() throws Exception {
        setupReservationJustStarted();
		reservation.startAndWait();
		reservation.stopAndWait();
		verify(reservationEventBus).stopAndWait();
	}

	@Test
	public void testThatEventStoreIsStartedWhenStartingReservation() throws Exception {
        setupReservationJustStarted();
		reservation.startAndWait();
		verify(reservationEventStore).startAndWait();
	}

	@Test
	public void testThatEventStoreIsStoppedWhenStoppingReservation() throws Exception {
        setupReservationJustStarted();
		reservation.startAndWait();
		reservation.stopAndWait();
		verify(reservationEventStore).stopAndWait();
	}


    @Test
    public void testThatOngoingReservationIsStartedImmedeatly() throws Exception {
        setupReservationOngoing();
        verify(schedulerService).schedule(isA(ReservationImpl.ReservationMadeCallable.class), eq(0l), any(TimeUnit.class));

        // TODO test life cycle callables
        //verify(schedulerService).schedule(isA(ReservationImpl.ReservationStartCallable.class), eq(0l), any(TimeUnit.class));

    }

    @Test
    public void testThatMadeReservationIsNotStartedIfFinalized() throws Exception {
        setupReservationEndedAndFinalized();
        //when(res.getFinalized()).thenReturn(DateTime.now().minusHours(1));

        verify(schedulerService, never()).schedule(any(Callable.class), anyLong(), any(TimeUnit.class));


    }

    @Test
    public void testPastEventsForNormallyEndedAndFinalizedReservation() {
        setupReservationEndedAndFinalized();
        final List<MessageLite> pastLifecycleEvents = reservation.getPastLifecycleEvents();

        assertEquals(4, pastLifecycleEvents.size());

        assertTrue(pastLifecycleEvents.get(0) instanceof ReservationMadeEvent);
        assertTrue(pastLifecycleEvents.get(1) instanceof ReservationStartedEvent);
        assertTrue(pastLifecycleEvents.get(2) instanceof ReservationEndedEvent);
        assertTrue(pastLifecycleEvents.get(3) instanceof ReservationFinalizedEvent);


    }

    @Test
    public void testPastEventsForOngoingReservation() {
        setupReservationOngoing();
        final List<MessageLite> pastLifecycleEvents = reservation.getPastLifecycleEvents();

        assertEquals(2, pastLifecycleEvents.size());

        assertTrue(pastLifecycleEvents.get(0) instanceof ReservationMadeEvent);
        assertTrue(pastLifecycleEvents.get(1) instanceof ReservationStartedEvent);


    }


    @Test
    public void testPastEventsForEndedButNonFinalizedReservation() {
        setupReservationEnded();
        final List<MessageLite> pastLifecycleEvents = reservation.getPastLifecycleEvents();

        assertEquals(3, pastLifecycleEvents.size());

        assertTrue(pastLifecycleEvents.get(0) instanceof ReservationMadeEvent);
        assertTrue(pastLifecycleEvents.get(1) instanceof ReservationStartedEvent);
        assertTrue(pastLifecycleEvents.get(2) instanceof ReservationEndedEvent);

    }

    @Test
    public void testPastEventsForNotYetStartedReservation() {
        setupReservationNotYetStarted();
        final List<MessageLite> pastLifecycleEvents = reservation.getPastLifecycleEvents();

        assertEquals(1, pastLifecycleEvents.size());

        assertTrue(pastLifecycleEvents.get(0) instanceof ReservationMadeEvent);

    }

    @Test
    public void testPastEventsCancelledBeforeStartedReservation() {
        setupReservationCancelledBeforeStart();
        final List<MessageLite> pastLifecycleEvents = reservation.getPastLifecycleEvents();

        assertEquals(2, pastLifecycleEvents.size());

        assertTrue(pastLifecycleEvents.get(0) instanceof ReservationMadeEvent);
        assertTrue(pastLifecycleEvents.get(1) instanceof ReservationCancelledEvent);

    }

    @Test
    public void testPastEventsCancelledAfterStartReservation() {
        setupReservationCancelledAfterStart();
        final List<MessageLite> pastLifecycleEvents = reservation.getPastLifecycleEvents();

        assertEquals(3, pastLifecycleEvents.size());

        assertTrue(pastLifecycleEvents.get(0) instanceof ReservationMadeEvent);
        assertTrue(pastLifecycleEvents.get(1) instanceof ReservationStartedEvent);
        assertTrue(pastLifecycleEvents.get(2) instanceof ReservationCancelledEvent);

    }

}
