package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.refEq;
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

	@Mock
	private PortalServerConfig portalServerConfig;

	private ReservationImpl reservation;

	private MessageFactory messageFactory;

	@Before
	public void setUp() throws Exception {

		messageFactory = new MessageFactoryImpl(new IncrementalIdProvider(), new UnixTimestampProvider());

		when(reservationEventBusFactory.create(Matchers.<Reservation>any())).thenReturn(reservationEventBus);
		when(reservationEventStoreFactory.createOrLoad(Matchers.<Reservation>any())).thenReturn(reservationEventStore);
		when(portalServerConfig.isReservationEventStoreEnabled()).thenReturn(true);

		when(reservationEventStore.startAsync()).thenReturn(reservationEventStore);
		when(reservationEventStore.stopAsync()).thenReturn(reservationEventStore);

		when(reservationEventBus.startAsync()).thenReturn(reservationEventBus);
		when(reservationEventBus.stopAsync()).thenReturn(reservationEventBus);
	}

	private void setupReservationJustStarted() {
		reservation = new ReservationImpl(
				commonConfig,
				portalServerConfig,
				rsPersistence,
				reservationEventBusFactory,
				responseTrackerTimedCache,
				responseTrackerFactory,
				reservationEventStoreFactory,
				portalEventBus,
				messageFactory,
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
				portalServerConfig,
				rsPersistence,
				reservationEventBusFactory,
				responseTrackerTimedCache,
				responseTrackerFactory,
				reservationEventStoreFactory,
				portalEventBus,
				messageFactory,
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
				portalServerConfig,
				rsPersistence,
				reservationEventBusFactory,
				responseTrackerTimedCache,
				responseTrackerFactory,
				reservationEventStoreFactory,
				portalEventBus,
				messageFactory,
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
				portalServerConfig,
				rsPersistence,
				reservationEventBusFactory,
				responseTrackerTimedCache,
				responseTrackerFactory,
				reservationEventStoreFactory,
				portalEventBus,
				messageFactory,
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
				portalServerConfig,
				rsPersistence,
				reservationEventBusFactory,
				responseTrackerTimedCache,
				responseTrackerFactory,
				reservationEventStoreFactory,
				portalEventBus,
				messageFactory,
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
				portalServerConfig,
				rsPersistence,
				reservationEventBusFactory,
				responseTrackerTimedCache,
				responseTrackerFactory,
				reservationEventStoreFactory,
				portalEventBus,
				messageFactory,
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
				portalServerConfig,
				rsPersistence,
				reservationEventBusFactory,
				responseTrackerTimedCache,
				responseTrackerFactory,
				reservationEventStoreFactory,
				portalEventBus,
				messageFactory,
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

	private void setupReservationCancelledAfterEnd() {
		reservation = new ReservationImpl(
				commonConfig,
				portalServerConfig,
				rsPersistence,
				reservationEventBusFactory,
				responseTrackerTimedCache,
				responseTrackerFactory,
				reservationEventStoreFactory,
				portalEventBus,
				messageFactory,
				schedulerService,
				newArrayList(confidentialReservationData),
				"someRandomReservationIdHere",
				username,
				DateTime.now().minusHours(1),
				null,
				NODE_URNS,
				INTERVAL_IN_THE_PAST
		);
	}

	@Test
	public void testThatReservationEventBusIsCreatedAndStartedWhenStartingReservation() throws Exception {
		setupReservationJustStarted();
		reservation.startAsync().awaitRunning();
		verify(reservationEventBus).startAsync();
		verify(reservationEventBus).awaitRunning();
	}

	@Test
	public void testThatReservationEventBusIsStoppedWhenStoppingReservation() throws Exception {
		setupReservationJustStarted();
		reservation.startAsync().awaitRunning();
		reservation.stopAsync().awaitTerminated();
		verify(reservationEventBus).stopAsync();
		verify(reservationEventBus).awaitTerminated();
	}

	@Test
	public void testThatEventStoreIsStartedWhenStartingReservation() throws Exception {
		setupReservationJustStarted();
		reservation.startAsync().awaitRunning();
		verify(reservationEventStore).startAsync();
		verify(reservationEventStore).awaitRunning();
	}

	@Test
	public void testThatEventStoreIsStoppedWhenStoppingReservation() throws Exception {
		setupReservationJustStarted();
		reservation.startAsync().awaitRunning();
		reservation.stopAsync().awaitTerminated();
		verify(reservationEventStore).stopAsync();
		verify(reservationEventStore).awaitTerminated();
	}


	@Test
	public void testThatOngoingReservationIsStartedImmediately() throws Exception {
		setupReservationOngoing();
		reservation.startAsync().awaitRunning();
		verify(schedulerService).execute(refEq(reservation.startRunnable));
		reservation.startRunnable.run();
		verify(schedulerService).schedule(isA(ReservationImpl.ReservationMadeCallable.class), eq(0l), any(TimeUnit.class));
	}

	@Test
	public void testIfOpenedEventIsPostedOnStartAsynchronously() throws Exception {
		setupReservationOngoing();
		reservation.startAsync().awaitRunning();
		verify(schedulerService).execute(refEq(reservation.startRunnable));
		reservation.startRunnable.run();

		// test if one upstream and one downstream event are send
		ArgumentCaptor<ReservationOpenedEvent> events = ArgumentCaptor.forClass(ReservationOpenedEvent.class);
		verify(portalEventBus, times(2)).post(events.capture());
		assertTrue(events.getAllValues().get(0).getHeader().getUpstream());
		assertTrue(events.getAllValues().get(1).getHeader().getDownstream());
	}

	@Test
	public void testIfClosedEventIsPostedOnStop() throws Exception {
		setupReservationOngoing();
		reservation.startAsync().awaitRunning();
		reservation.stopAsync().awaitTerminated();
		ArgumentCaptor<ReservationClosedEvent> events = ArgumentCaptor.forClass(ReservationClosedEvent.class);
		verify(portalEventBus, times(2)).post(events.capture());
		assertTrue(events.getAllValues().get(0).getHeader().getUpstream());
		assertTrue(events.getAllValues().get(1).getHeader().getDownstream());
	}

	@Test
	public void testThatMadeReservationIsNotStartedIfFinalized() throws Exception {
		setupReservationEndedAndFinalized();

		//noinspection unchecked
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


	@Test
	public void testMadeCallableForOngoingReservation() throws Exception {
		setupReservationOngoing();
		ReservationImpl.ReservationMadeCallable callable = reservation.new ReservationMadeCallable();

		callable.call();
		ArgumentCaptor<ReservationMadeEvent> events = ArgumentCaptor.forClass(ReservationMadeEvent.class);
		verify(portalEventBus, times(2)).post(events.capture());
		assertTrue(events.getAllValues().get(0).getHeader().getUpstream());
		assertTrue(events.getAllValues().get(1).getHeader().getDownstream());
		verify(schedulerService).schedule(isA(ReservationImpl.ReservationStartCallable.class), eq(0l), any(TimeUnit.class));
	}

	@Test
	public void testStartCallableForEndedReservation() throws Exception {
		setupReservationEnded();
		ReservationImpl.ReservationStartCallable callable = reservation.new ReservationStartCallable();

		callable.call();
		ArgumentCaptor<ReservationStartedEvent> events = ArgumentCaptor.forClass(ReservationStartedEvent.class);
		verify(portalEventBus, times(2)).post(events.capture());
		assertTrue(events.getAllValues().get(0).getHeader().getUpstream());
		assertTrue(events.getAllValues().get(1).getHeader().getDownstream());
		verify(schedulerService).schedule(isA(ReservationImpl.ReservationEndCallable.class), eq(0l), any(TimeUnit.class));
	}

	@Test
	public void testStartCallableForReservationCancelledBeforeEnd() throws Exception {
		setupReservationCancelledAfterStart();
		ReservationImpl.ReservationStartCallable callable = reservation.new ReservationStartCallable();

		callable.call();
		ArgumentCaptor<ReservationStartedEvent> events = ArgumentCaptor.forClass(ReservationStartedEvent.class);
		verify(portalEventBus, times(2)).post(events.capture());
		assertTrue(events.getAllValues().get(0).getHeader().getUpstream());
		assertTrue(events.getAllValues().get(1).getHeader().getDownstream());
		verify(schedulerService).schedule(isA(ReservationImpl.ReservationCancelCallable.class), anyLong(), any(TimeUnit.class));
	}

	@Test
	public void testStartCallableForReservationCancelledAfterEnd() throws Exception {
		setupReservationCancelledAfterEnd();
		ReservationImpl.ReservationStartCallable callable = reservation.new ReservationStartCallable();

		callable.call();
		ArgumentCaptor<ReservationStartedEvent> events = ArgumentCaptor.forClass(ReservationStartedEvent.class);
		verify(portalEventBus, times(2)).post(events.capture());
		assertTrue(events.getAllValues().get(0).getHeader().getUpstream());
		assertTrue(events.getAllValues().get(1).getHeader().getDownstream());
		verify(schedulerService).schedule(isA(ReservationImpl.ReservationEndCallable.class), anyLong(), any(TimeUnit.class));
		verify(schedulerService, never()).schedule(isA(ReservationImpl.ReservationCancelCallable.class), anyLong(), any(TimeUnit.class));
	}

	@Test
	public void testStartCallableForFinalizedReservation() throws Exception {
		setupReservationEndedAndFinalized();
		ReservationImpl.ReservationStartCallable callable = reservation.new ReservationStartCallable();

		callable.call();

		verify(portalEventBus, never()).post(isA(ReservationStartedEvent.class));
		verify(schedulerService).schedule(isA(ReservationImpl.ReservationFinalizeCallable.class), anyLong(), any(TimeUnit.class));
	}

	@Test
	public void testEndCallableForEndedReservation() throws Exception {
		setupReservationEnded();
		ReservationImpl.ReservationEndCallable callable = reservation.new ReservationEndCallable();

		callable.call();
		ArgumentCaptor<ReservationEndedEvent> events = ArgumentCaptor.forClass(ReservationEndedEvent.class);
		verify(portalEventBus, times(2)).post(events.capture());
		assertTrue(events.getAllValues().get(0).getHeader().getUpstream());
		assertTrue(events.getAllValues().get(1).getHeader().getDownstream());
		verify(schedulerService).schedule(isA(ReservationImpl.ReservationFinalizeCallable.class), anyLong(), any(TimeUnit.class));
	}

	@Test
	public void testFinalizeCallableForNonFinalizedReservation() throws Exception {
		setupReservationEnded();
		when(commonConfig.getUrnPrefix()).thenReturn(new NodeUrn("urn:unit-test:0x0001").getPrefix());
		ReservationImpl.ReservationFinalizeCallable callable = reservation.new ReservationFinalizeCallable();

		callable.call();
		ArgumentCaptor<ReservationFinalizedEvent> events = ArgumentCaptor.forClass(ReservationFinalizedEvent.class);
		verify(portalEventBus, times(2)).post(events.capture());
		assertTrue(events.getAllValues().get(0).getHeader().getUpstream());
		assertTrue(events.getAllValues().get(1).getHeader().getDownstream());
		assertFalse(reservation.isRunning());
	}
}
