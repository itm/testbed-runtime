package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.iwsn.messages.*;
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

import java.nio.charset.Charset;
import java.util.Random;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.*;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

// FIXME adapt test to PortalEventDispatcher
@RunWith(MockitoJUnitRunner.class)
public class ReservationEventBusImplTest {

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

	static {
		LINKS_RESERVED.put(GW1_N1, GW1_N2);
		LINKS_RESERVED.put(GW2_N1, GW2_N3);

		LINKS_UNRESERVED.put(GW1_N1, GW2_N2);
		LINKS_UNRESERVED.put(GW2_N2, GW2_N3);
	}

	private static final Random RANDOM = new Random();

	private static final Iterable<? extends ChannelHandlerConfiguration>
			CHANNEL_HANDLER_CONFIGURATIONS = newArrayList();

	private static final String RESERVATION_ID = "" + RANDOM.nextLong();

	private static final String OTHER_RESERVATION_ID = "" + RANDOM.nextLong();

	private static final Set<Reservation.Entry> ENTRIES = newHashSet(
			new Reservation.Entry(GW1_N1.getPrefix(), null, RESERVATION_ID, GW1_NODES, new Interval(0, 1), null),
			new Reservation.Entry(GW2_N1.getPrefix(), null, RESERVATION_ID, GW1_NODES, new Interval(0, 1), null)
	);

	@Mock
	private PortalEventBus portalEventBus;

	@Mock
	private EventBusFactory eventBusFactory;

	@Mock
	private EventBus eventBus;

	@Mock
	private Reservation reservation;

	private ReservationEventBusImpl reservationEventBus;

	@Before
	public void setUp() throws Exception {
		when(reservation.getNodeUrns()).thenReturn(RESERVED_NODES);
		when(reservation.getSerializedKey()).thenReturn(RESERVATION_ID);
        when(eventBusFactory.create(anyString())).thenReturn(eventBus);
        reservationEventBus = new ReservationEventBusImpl(portalEventBus, eventBusFactory, reservation);
		reservationEventBus.startAndWait();
	}

	@After
	public void tearDown() throws Exception {
		reservationEventBus.stopAndWait();
		reservationEventBus = null;
	}

	@Test
	public void testAreNodesAliveRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testAreNodesAliveRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
	}

	@Test
	public void testAreNodesConnectedRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testAreNodesConnectedRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
	}

	@Test
	public void testDisableNodesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testDisableNodesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
	}

	@Test
	public void testEnableNodesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testEnableNodesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
	}

	@Test
	public void testResetNodesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testResetNodesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
	}

	@Test
	public void testDisableVirtualLinksRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testDisableVirtualLinksRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
	}

	@Test
	public void testEnableVirtualLinksRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testEnableVirtualLinksRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
	}

	@Test
	public void testDisablePhysicalLinksRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testDisablePhysicalLinksRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
	}

	@Test
	public void testEnablePhysicalLinksRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
	}

	@Test
	public void testEnablePhysicalLinksRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {

	}

	@Test
	public void testSendDownstreamMessageRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {

	}

	@Test
	public void testSendDownstreamMessageRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {

	}

	@Test
	public void testFlashImagesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {

	}

	@Test
	public void testFlashImagesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {

	}

	@Test
	public void testSetChannelPipelinesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {

	}

	@Test
	public void testSetChannelPipelinesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {

	}

	@Test
	public void testUpstreamMessageEventShouldBeForwardedIfNodePartOfReservation() throws Exception {

	}

	@Test
	public void testUpstreamMessageEventShouldNotBeForwardedIfNodeNotPartOfReservation() throws Exception {

	}

	@Test
	public void testDevicesAttachedEventShouldBeForwardedIfNodePartOfReservation() throws Exception {

	}

	@Test
	public void testDevicesAttachedEventShouldNotBeForwardedIfNodeNotPartOfReservation() throws Exception {

	}

	@Test
	public void testDevicesAttachedEventShouldBeFilteredIfSomeNodesArePartOfReservation() throws Exception {
	}

	@Test
	public void testSingleNodeProgressShouldBeForwardedIfHasSameReservationId() throws Exception {
	}

	@Test
	public void testSingleNodeProgressDetachedEventShouldNotBeForwardedIfHasOtherReservationId() throws Exception {
	}

	@Test
	public void testSingleNodeResponseShouldBeForwardedIfHasSameReservationId() throws Exception {
	}

	@Test
	public void testSingleNodeResponseDetachedEventShouldNotBeForwardedIfHasOtherReservationId() throws Exception {
	}

	@Test
	public void testNotificationShouldBeForwardedIfNoNodeIsSet() throws Exception {
	}

	@Test
	public void testNotificationShouldBeForwardedIfNodeIsPartOfReservation() throws Exception {
	}

	@Test
	public void testNotificationShouldNotBeForwardedIfNodeIsNotPartOfReservation() throws Exception {
	}

	@Test
	public void testDevicesDetachedEventShouldBeForwardedIfNodePartOfReservation() throws Exception {
	}

	@Test
	public void testDevicesDetachedEventShouldNotBeForwardedIfNodeNotPartOfReservation() throws Exception {
	}

	@Test
	public void testDevicesDetachedEventShouldBeFilteredIfSomeNodesArePartOfReservation() throws Exception {
	}

	private UpstreamMessageEvent newUpstreamMessageEvent(final NodeUrn sourceNodeUrn, final String message) {
		return UpstreamMessageEvent.newBuilder()
				.setSourceNodeUrn(sourceNodeUrn.toString())
				.setTimestamp(new DateTime().getMillis())
				.setMessageBytes(ByteString.copyFrom(message.getBytes()))
				.build();
	}

	private void postRequestAndVerifyForwarded(final Request request) {
		reservationEventBus.post(request);
		verify(eventBus).post(eq(request));
	}

	private void postRequestAndVerifyException(final Request request) {
		try {
			reservationEventBus.post(request);
			fail("An IllegalArgumentException should have been thrown!");
		} catch (IllegalArgumentException expected) {
		}
	}
}
