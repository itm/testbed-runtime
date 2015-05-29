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

import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PortalEventDispatcherImplTest {

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

	private static final java.util.Optional<Long> NOW = java.util.Optional.of(System.currentTimeMillis());

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

	private PortalEventDispatcherImpl portalEventDispatcher;

	private MessageFactory mf;

	@Before
	public void setUp() throws Exception {

		when(reservation.getNodeUrns()).thenReturn(RESERVED_NODES);
		when(reservation.getSerializedKey()).thenReturn(RESERVATION_ID);
		when(reservation.getEventBus()).thenReturn(eventBus);

		when(reservationManager.getReservation(any(NodeUrn.class), any(DateTime.class))).thenReturn(com.google.common.base.Optional.<Reservation>absent());
		for (NodeUrn node : RESERVED_NODES) {
			when(reservationManager.getReservation(eq(node), any(DateTime.class))).thenReturn(com.google.common.base.Optional.of(reservation));
		}

		//noinspection unchecked
		when(reservationManager.getReservation(any(Set.class))).thenReturn(reservation);
		when(reservationManager.getReservation(RESERVATION_ID)).thenReturn(reservation);
		when(reservationManager.getReservations(any(DateTime.class))).thenReturn(newArrayList(reservation));

		portalEventDispatcher = new PortalEventDispatcherImpl(portalEventBus, reservationManager, portalEventStore);
		mf = new MessageFactoryImpl(new IncrementalIdProvider(), new UnixTimestampProvider());
	}

	@Test
	public void testUpstreamMessageEventShouldBeForwardedIfNodePartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			final UpstreamMessageEvent event = mf.upstreamMessageEvent(NOW, nodeUrn, nodeUrn.toString().getBytes());
			portalEventDispatcher.onUpstreamMessageEventFromPortalEventBus(event);
			verify(eventBus).post(eq(event));
		}
	}

	@Test
	public void testUpstreamMessageEventShouldNotBeForwardedIfNodeNotPartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final UpstreamMessageEvent event = mf.upstreamMessageEvent(NOW, nodeUrn, nodeUrn.toString().getBytes());
			portalEventDispatcher.onUpstreamMessageEventFromPortalEventBus(event);
			verify(eventBus, never()).post(eq(event));
		}
	}

	@Test
	public void testDevicesAttachedEventShouldBeForwardedIfNodePartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			final DevicesAttachedEvent event = mf.devicesAttachedEvent(NOW, nodeUrn);
			portalEventDispatcher.onDevicesAttachedEventFromPortalEventBus(event);
			verify(eventBus).post(eq(event));
		}
	}

	@Test
	public void testDevicesAttachedEventShouldNotBeForwardedIfNodeNotPartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final DevicesAttachedEvent event = mf.devicesAttachedEvent(NOW, nodeUrn);
			portalEventDispatcher.onDevicesAttachedEventFromPortalEventBus(event);
			verify(eventBus, never()).post(eq(event));
		}
	}

	@Test
	public void testDevicesAttachedEventShouldBeStoredIfNotMatchingReservationFound() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final DevicesAttachedEvent event = mf.devicesAttachedEvent(NOW, nodeUrn);
			portalEventDispatcher.onDevicesAttachedEventFromPortalEventBus(event);
			//noinspection unchecked
			verify(portalEventStore).storeEvent(event, event.getClass(), event.getHeader().getTimestamp());
		}
	}

	@Test
	public void testDevicesAttachedEventShouldBeFilteredIfSomeNodesArePartOfReservation() throws Exception {

		final DevicesAttachedEvent event = mf.devicesAttachedEvent(NOW, ALL_NODES);
		portalEventDispatcher.onDevicesAttachedEventFromPortalEventBus(event);

		final ArgumentCaptor<DevicesAttachedEvent> captor = ArgumentCaptor.forClass(DevicesAttachedEvent.class);
		verify(eventBus, times(1)).post(captor.capture());
		assertTrue(MessageUtils.equals(captor.getValue(), mf.devicesAttachedEvent(NOW, RESERVED_NODES)));
	}

	@Test
	public void testSingleNodeProgressShouldBeForwardedIfHasSameReservationId() throws Exception {
		when(reservation.getEntries()).thenReturn(ENTRIES);
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			final SingleNodeProgress progress = mf.progress(
					Optional.of(RESERVATION_ID),
					Optional.of(RANDOM.nextLong()),
					MessageType.REQUEST_ARE_NODES_ALIVE,
					123,
					nodeUrn,
					37
			);
			portalEventDispatcher.onSingleNodeProgressFromPortalEventBus(progress);
			verify(eventBus).post(eq(progress));
		}
	}

	@Test
	public void testSingleNodeProgressDetachedEventShouldNotBeForwardedIfHasOtherReservationId() throws Exception {
		when(reservation.getEntries()).thenReturn(ENTRIES);
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final SingleNodeProgress progress = mf.progress(
					Optional.of(OTHER_RESERVATION_ID),
					Optional.of(RANDOM.nextLong()),
					MessageType.REQUEST_ARE_NODES_ALIVE,
					123,
					nodeUrn,
					37
			);
			portalEventDispatcher.onSingleNodeProgressFromPortalEventBus(progress);
			verify(eventBus, never()).post(eq(progress));
		}
	}

	@Test
	public void testSingleNodeProgressDetachedEventShouldBeStoredIfNotMatchingReservationFound() throws Exception {
		when(reservation.getEntries()).thenReturn(ENTRIES);
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final SingleNodeProgress progress = mf.progress(
					Optional.of(OTHER_RESERVATION_ID),
					Optional.of(RANDOM.nextLong()),
					MessageType.REQUEST_ARE_NODES_ALIVE,
					123,
					nodeUrn,
					37
			);
			portalEventDispatcher.onSingleNodeProgressFromPortalEventBus(progress);
			//noinspection unchecked
			verify(portalEventStore).storeEvent(progress, progress.getClass());
		}
	}

	@Test
	public void testSingleNodeResponseShouldBeForwardedIfHasSameReservationId() throws Exception {
		when(reservation.getEntries()).thenReturn(ENTRIES);
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			final SingleNodeResponse response = mf.response(
					Optional.of(RESERVATION_ID),
					Optional.of(RANDOM.nextLong()),
					MessageType.REQUEST_ARE_NODES_ALIVE,
					123,
					nodeUrn,
					37,
					Optional.empty()
			);
			portalEventDispatcher.onSingleNodeResponseFromPortalEventBus(response);
			verify(eventBus).post(eq(response));
		}
	}

	@Test
	public void testSingleNodeResponseDetachedEventShouldNotBeForwardedIfHasOtherReservationId() throws Exception {
		when(reservation.getEntries()).thenReturn(ENTRIES);
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final SingleNodeResponse response = mf.response(
					Optional.of(OTHER_RESERVATION_ID),
					Optional.of(RANDOM.nextLong()),
					MessageType.REQUEST_ARE_NODES_ALIVE,
					123,
					nodeUrn,
					37,
					Optional.empty()
			);
			portalEventDispatcher.onSingleNodeResponseFromPortalEventBus(response);
			verify(eventBus, never()).post(eq(response));
		}
	}

	@Test
	public void testSingleNodeResponseDetachedEventShouldBeStoredIfNotMatchingAReservation() throws Exception {
		when(reservation.getEntries()).thenReturn(ENTRIES);
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final SingleNodeResponse response = mf.response(
					Optional.of(OTHER_RESERVATION_ID),
					Optional.of(RANDOM.nextLong()),
					MessageType.REQUEST_ARE_NODES_ALIVE,
					123,
					nodeUrn,
					37,
					Optional.empty()
			);
			portalEventDispatcher.onSingleNodeResponseFromPortalEventBus(response);

			//noinspection unchecked
			verify(portalEventStore).storeEvent(response, response.getClass());
		}
	}

	@Test
	public void testNotificationShouldBeForwardedIfNoNodeIsSet() throws Exception {
		final NotificationEvent event = mf.notificationEvent(Optional.empty(), NOW, "hello");
		portalEventDispatcher.onNotificationEventFromPortalEventBus(event);
		verify(eventBus).post(eq(event));
	}

	@Test
	public void testNotificationShouldBeForwardedIfNodeIsPartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			final NotificationEvent event = mf.notificationEvent(Optional.of(nodeUrn), NOW, nodeUrn.toString());
			portalEventDispatcher.onNotificationEventFromPortalEventBus(event);
			verify(eventBus).post(eq(event));
		}
	}

	@Test
	public void testNotificationShouldNotBeForwardedIfNodeIsNotPartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final NotificationEvent event = mf.notificationEvent(Optional.of(nodeUrn), NOW, nodeUrn.toString());
			portalEventDispatcher.onNotificationEventFromPortalEventBus(event);
			verify(eventBus, never()).post(eq(event));
		}
	}


	@Test
	public void testNotificationShouldBeStoredIfNoMatchingReservationFound() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final NotificationEvent event = mf.notificationEvent(Optional.of(nodeUrn), NOW, nodeUrn.toString());
			portalEventDispatcher.onNotificationEventFromPortalEventBus(event);
			//noinspection unchecked
			verify(portalEventStore).storeEvent(event, event.getClass(), event.getHeader().getTimestamp());
		}
	}

	@Test
	public void testDevicesDetachedEventShouldBeForwardedIfNodePartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : RESERVED_NODES) {
			final DevicesDetachedEvent event = mf.devicesDetachedEvent(NOW, nodeUrn);
			portalEventDispatcher.onDevicesDetachedEventFromPortalEventBus(event);
			verify(eventBus).post(eq(event));
		}
	}

	@Test
	public void testDevicesDetachedEventShouldNotBeForwardedIfNodeNotPartOfReservation() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final DevicesDetachedEvent event = mf.devicesDetachedEvent(NOW, nodeUrn);
			portalEventDispatcher.onDevicesDetachedEventFromPortalEventBus(event);
			verify(eventBus, never()).post(eq(event));
		}
	}

	@Test
	public void testDevicesDetachedEventShouldBeStoredIfNoMatchingReservationFound() throws Exception {
		for (NodeUrn nodeUrn : UNRESERVED_NODES) {
			final DevicesDetachedEvent event = mf.devicesDetachedEvent(NOW, nodeUrn);
			portalEventDispatcher.onDevicesDetachedEventFromPortalEventBus(event);
			//noinspection unchecked
			verify(portalEventStore).storeEvent(event, event.getClass(), event.getHeader().getTimestamp());
		}
	}


	@Test
	public void testDevicesDetachedEventShouldBeFilteredIfSomeNodesArePartOfReservation() throws Exception {

		final DevicesDetachedEvent event = mf.devicesDetachedEvent(NOW, ALL_NODES);
		portalEventDispatcher.onDevicesDetachedEventFromPortalEventBus(event);

		final ArgumentCaptor<DevicesDetachedEvent> captor = ArgumentCaptor.forClass(DevicesDetachedEvent.class);
		verify(eventBus, times(1)).post(captor.capture());
		assertTrue(MessageUtils.equals(captor.getValue(), mf.devicesDetachedEvent(NOW, RESERVED_NODES)));
	}

	@After
	public void tearDown() throws Exception {
		portalEventDispatcher.stopAsync().awaitTerminated();
		portalEventDispatcher = null;
	}
}
