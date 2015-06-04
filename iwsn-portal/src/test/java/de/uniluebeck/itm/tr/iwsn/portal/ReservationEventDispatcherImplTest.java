package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStore;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Random;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReservationEventDispatcherImplTest {

	private static final MessageFactory MESSAGE_FACTORY = new MessageFactoryImpl(
			new IncrementalIdProvider(),
			new UnixTimestampProvider()
	);

	private static final NodeUrn GW1_N1 = new NodeUrn("urn:unit-test:gw1:0x0001");
	private static final NodeUrn GW1_N2 = new NodeUrn("urn:unit-test:gw1:0x0002");
	private static final NodeUrn GW1_N3 = new NodeUrn("urn:unit-test:gw1:0x0003");

	private static final NodeUrn GW2_N1 = new NodeUrn("urn:unit-test:gw2:0x0001");
	private static final NodeUrn GW2_N2 = new NodeUrn("urn:unit-test:gw2:0x0002");
	private static final NodeUrn GW2_N3 = new NodeUrn("urn:unit-test:gw2:0x0003");

	private static final Set<NodeUrn> GW1_NODES = newHashSet(GW1_N1, GW1_N2, GW1_N3);
	private static final Set<NodeUrn> GW2_NODES = newHashSet(GW2_N1, GW2_N2, GW2_N3);
	private static final Set<NodeUrn> ALL_NODES = union(GW1_NODES, GW2_NODES);
	private static final Set<NodeUrn> RESERVED_NODES = newHashSet(GW1_N1, GW1_N2, GW2_N1, GW2_N3);
	private static final Set<NodeUrn> UNRESERVED_NODES = difference(ALL_NODES, RESERVED_NODES);

	private static final Multimap<NodeUrn, NodeUrn> LINKS_RESERVED = HashMultimap.create();
	private static final Multimap<NodeUrn, NodeUrn> LINKS_UNRESERVED = HashMultimap.create();
	private static final Random RANDOM = new Random();
	private static final String RESERVATION_ID = "" + RANDOM.nextLong();
	private static final Set<Reservation.Entry> ENTRIES = newHashSet(
			new Reservation.Entry(GW1_N1.getPrefix(), null, RESERVATION_ID, GW1_NODES, new Interval(0, 1), null),
			new Reservation.Entry(GW2_N1.getPrefix(), null, RESERVATION_ID, GW1_NODES, new Interval(0, 1), null)
	);
	private static final String OTHER_RESERVATION_ID = "" + RANDOM.nextLong();

	private static final java.util.Optional<Long> NOW = of(System.currentTimeMillis());

	static {
		LINKS_RESERVED.put(GW1_N1, GW1_N2);
		LINKS_RESERVED.put(GW2_N1, GW2_N3);

		LINKS_UNRESERVED.put(GW1_N1, GW2_N2);
		LINKS_UNRESERVED.put(GW2_N2, GW2_N3);
	}

	@Mock
	private PortalEventBus portalEventBus;

	@Mock
	private ReservationEventBus eventBus;

	@Mock
	private EventBusFactory eventBusFactory;

	@Mock
	private Reservation reservation;

	@Mock
	private ReservationManager reservationManager;

	@Mock
	private PortalEventStore portalEventStore;

	private ReservationEventDispatcherImpl portalEventDispatcher;

	private MessageFactory mf;

	@Before
	public void setUp() throws Exception {

		when(reservation.getNodeUrns()).thenReturn(RESERVED_NODES);
		when(reservation.getSerializedKey()).thenReturn(RESERVATION_ID);
		when(reservation.getEventBus()).thenReturn(eventBus);

		when(reservationManager.getReservation(any(NodeUrn.class), any(DateTime.class))).thenReturn(empty());
		for (NodeUrn node : RESERVED_NODES) {
			when(reservationManager.getReservation(eq(node), any(DateTime.class))).thenReturn(of(reservation));
		}

		//noinspection unchecked
		when(reservationManager.getReservation(any(Set.class))).thenReturn(reservation);
		when(reservationManager.getReservation(RESERVATION_ID)).thenReturn(reservation);
		when(reservationManager.getReservations(any(DateTime.class))).thenReturn(newArrayList(reservation));

		portalEventDispatcher = new ReservationEventDispatcherImpl(portalEventBus, reservationManager, MESSAGE_FACTORY);
		mf = new MessageFactoryImpl(new IncrementalIdProvider(), new UnixTimestampProvider());
	}

	@Test
	public void testEventShouldBeForwardedIfNodePartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			setUpMapping(nodeUrn);
			final UpstreamMessageEvent event = mf.upstreamMessageEvent(NOW, nodeUrn, nodeUrn.toString().getBytes());
			portalEventDispatcher.onEvent(event);
			verify(eventBus).post(eq(event));
		}
	}

	@Test
	public void testEventShouldNotBeForwardedIfNodeNotPartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			when(reservationManager.getReservationMapping(any(), any())).thenReturn(HashMultimap.create());
			final UpstreamMessageEvent event = mf.upstreamMessageEvent(NOW, nodeUrn, nodeUrn.toString().getBytes());
			portalEventDispatcher.onEvent(event);
			verify(eventBus, never()).post(eq(event));
		}
	}

	@Test
	public void testEventShouldBeFilteredIfSomeNodesArePartOfReservation() throws Exception {

		Multimap<Reservation, NodeUrn> mapping = HashMultimap.create();
		mapping.putAll(reservation, RESERVED_NODES);
		when(reservationManager.getReservationMapping(any(), eq(new DateTime(NOW.get())))).thenReturn(mapping);

		final DevicesAttachedEvent event = mf.devicesAttachedEvent(NOW, ALL_NODES);
		portalEventDispatcher.onEvent(event);

		final ArgumentCaptor<DevicesAttachedEvent> captor = ArgumentCaptor.forClass(DevicesAttachedEvent.class);
		verify(eventBus, times(1)).post(captor.capture());
		assertTrue(MessageUtils.equals(captor.getValue(), mf.devicesAttachedEvent(NOW, RESERVED_NODES)));
	}

	@Test
	public void testProgressShouldBeForwardedIfHasSameReservationId() throws Exception {
		when(reservation.getEntries()).thenReturn(ENTRIES);
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			setUpMapping(nodeUrn);
			final Progress progress = mf.progress(
					of(RESERVATION_ID),
					of(RANDOM.nextLong()),
					123,
					newArrayList(nodeUrn),
					37
			);
			portalEventDispatcher.onEvent(progress);
			verify(eventBus).post(eq(progress));
		}
	}

	@Test
	public void testNotificationShouldBeForwardedIfNoNodeIsSet() throws Exception {
		final NotificationEvent event = mf.notificationEvent(empty(), NOW, "hello");
		portalEventDispatcher.onEvent(event);
		verify(eventBus).post(eq(event));
	}

	@Test
	public void testNotificationShouldBeForwardedIfNodeIsPartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			setUpMapping(nodeUrn);
			final NotificationEvent event = mf.notificationEvent(of(newArrayList(nodeUrn)), NOW, nodeUrn.toString());
			portalEventDispatcher.onEvent(event);
			verify(eventBus).post(eq(event));
		}
	}

	private void setUpMapping(NodeUrn nodeUrn) {
		Multimap<Reservation, NodeUrn> mapping = HashMultimap.create();
		mapping.put(reservation, nodeUrn);
		when(reservationManager.getReservationMapping(any(), any())).thenReturn(mapping);
	}

	@Test
	public void testNotificationShouldNotBeForwardedIfNodeIsNotPartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			when(reservationManager.getReservationMapping(any(), any())).thenReturn(HashMultimap.create());
			final NotificationEvent event = mf.notificationEvent(of(newArrayList(nodeUrn)), NOW, nodeUrn.toString());
			portalEventDispatcher.onEvent(event);
			verify(eventBus, never()).post(eq(event));
		}
	}

	@After
	public void tearDown() throws Exception {
		portalEventDispatcher.stopAsync().awaitTerminated();
		portalEventDispatcher = null;
	}
}
