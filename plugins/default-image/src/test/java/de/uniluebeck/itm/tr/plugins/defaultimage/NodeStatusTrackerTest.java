package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.common.collect.Sets;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.iwsn.common.BasicEventBusService;
import de.uniluebeck.itm.tr.iwsn.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import de.uniluebeck.itm.tr.rs.RSHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import static com.google.common.collect.Sets.*;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newFlashImagesRequest;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeStatusTrackerTest {

	private static final NodeUrn NODE_1 = new NodeUrn("urn:wisebed:uzl1:0x0001");

	private static final NodeUrn NODE_2 = new NodeUrn("urn:wisebed:uzl1:0x0002");

	private static final NodeUrn NODE_3 = new NodeUrn("urn:wisebed:uzl1:0x0003");

	private static final NodeUrn NODE_4 = new NodeUrn("urn:wisebed:uzl1:0x0004");

	private static final Set<NodeUrn> NODE_URNS = newHashSet(NODE_1, NODE_2, NODE_3, NODE_4);

	private static final Set<NodeUrn> RESERVED_NODE_URNS = newHashSet(NODE_1, NODE_2);

	private static final Set<NodeUrn> UNRESERVED_NODE_URNS = Sets.difference(NODE_URNS, RESERVED_NODE_URNS);

	private static final Duration MIN_UNRESERVED_DURATION = Duration.standardHours(1);

	@Mock
	private RSHelper rsHelper;

	@Mock
	private ServedNodeUrnsProvider servedNodeUrnsProvider;

	private EventBusService eventBusService;

	private NodeStatusTracker nodeStatusTracker;

	@Before
	public void setUp() throws Exception {

		when(rsHelper.getReservedNodes(eq(MIN_UNRESERVED_DURATION))).thenReturn(RESERVED_NODE_URNS);
		when(rsHelper.getUnreservedNodes(eq(MIN_UNRESERVED_DURATION))).thenReturn(UNRESERVED_NODE_URNS);
		when(servedNodeUrnsProvider.get()).thenReturn(NODE_URNS);

		eventBusService = new BasicEventBusService();
		eventBusService.startAndWait();

		nodeStatusTracker = new NodeStatusTrackerImpl(
				rsHelper,
				eventBusService,
				servedNodeUrnsProvider,
				MIN_UNRESERVED_DURATION
		);
		nodeStatusTracker.startAndWait();
	}

	@Test
	public void testStatusIsUnknownBeforeBeingCalledForTheFirstTime() throws Exception {

		for (NodeUrn nodeUrn : NODE_URNS) {
			assertEquals(FlashStatus.UNKNOWN, nodeStatusTracker.getFlashStatus(nodeUrn));
			assertEquals(ReservationStatus.UNKNOWN, nodeStatusTracker.getReservationStatus(nodeUrn));
		}

		assertEquals(Sets.<NodeUrn>newHashSet(), nodeStatusTracker.getNodes(FlashStatus.UNKNOWN));
		assertEquals(Sets.<NodeUrn>newHashSet(), nodeStatusTracker.getNodes(ReservationStatus.UNKNOWN));
	}

	@Test
	public void testReservationStatusIsCorrectAfterBeingCalledForTheFirstTime() throws Exception {

		nodeStatusTracker.run();

		for (NodeUrn nodeUrn : RESERVED_NODE_URNS) {
			assertEquals(ReservationStatus.RESERVED, nodeStatusTracker.getReservationStatus(nodeUrn));
		}

		for (NodeUrn nodeUrn : UNRESERVED_NODE_URNS) {
			assertEquals(ReservationStatus.UNRESERVED, nodeStatusTracker.getReservationStatus(nodeUrn));
		}

		assertEquals(RESERVED_NODE_URNS, nodeStatusTracker.getNodes(ReservationStatus.RESERVED));
		assertEquals(UNRESERVED_NODE_URNS, nodeStatusTracker.getNodes(ReservationStatus.UNRESERVED));
	}

	@Test
	public void testFlashStatusIsUpdatedAfterFlashOperation() throws Exception {
		nodeStatusTracker.run();
		eventBusService.post(newFlashImagesRequest(null, 123L, newHashSet(NODE_1), new byte[]{}));
		assertEquals(FlashStatus.USER_IMAGE, nodeStatusTracker.getFlashStatus(NODE_1));
	}

	@Test
	public void testFlashStatusIsDefaultImageAfterSet() throws Exception {
		nodeStatusTracker.run();
		eventBusService.post(newFlashImagesRequest(null, 123L, newHashSet(NODE_1), new byte[]{}));
		nodeStatusTracker.setFlashStatus(NODE_1, FlashStatus.DEFAULT_IMAGE);
		assertEquals(FlashStatus.DEFAULT_IMAGE, nodeStatusTracker.getFlashStatus(NODE_1));
	}

	@Test
	public void testReservationStatusIsUpdatedAfterReservationStartedEvent() throws Exception {

		nodeStatusTracker.run();

		final ReservationStartedEvent event = mock(ReservationStartedEvent.class);
		final Reservation reservation = mock(Reservation.class);
		when(event.getReservation()).thenReturn(reservation);
		when(reservation.getNodeUrns()).thenReturn(newHashSet(NODE_3));

		final Sets.SetView<NodeUrn> reservedAfterEvent = union(RESERVED_NODE_URNS, newHashSet(NODE_3));
		final Sets.SetView<NodeUrn> unreservedAfterEvent = difference(UNRESERVED_NODE_URNS, newHashSet(NODE_3));

		eventBusService.post(event);

		for (NodeUrn nodeUrn : reservedAfterEvent) {
			assertEquals(ReservationStatus.RESERVED, nodeStatusTracker.getReservationStatus(nodeUrn));
		}

		for (NodeUrn nodeUrn : unreservedAfterEvent) {
			assertEquals(ReservationStatus.UNRESERVED, nodeStatusTracker.getReservationStatus(nodeUrn));
		}

		assertEquals(reservedAfterEvent, nodeStatusTracker.getNodes(ReservationStatus.RESERVED));
		assertEquals(unreservedAfterEvent, nodeStatusTracker.getNodes(ReservationStatus.UNRESERVED));
	}

	@Test
	public void testReservationStatusIsUpdatedAfterReservationEndedEvent() throws Exception {

		nodeStatusTracker.run();

		final ReservationEndedEvent event = mock(ReservationEndedEvent.class);
		final Reservation reservation = mock(Reservation.class);
		when(event.getReservation()).thenReturn(reservation);
		when(reservation.getNodeUrns()).thenReturn(newHashSet(NODE_2));

		final Sets.SetView<NodeUrn> reservedAfterEvent = difference(RESERVED_NODE_URNS, newHashSet(NODE_2));
		final Sets.SetView<NodeUrn> unreservedAfterEvent = union(UNRESERVED_NODE_URNS, newHashSet(NODE_2));

		eventBusService.post(event);

		for (NodeUrn nodeUrn : reservedAfterEvent) {
			assertEquals(ReservationStatus.RESERVED, nodeStatusTracker.getReservationStatus(nodeUrn));
		}

		for (NodeUrn nodeUrn : unreservedAfterEvent) {
			assertEquals(ReservationStatus.UNRESERVED, nodeStatusTracker.getReservationStatus(nodeUrn));
		}

		assertEquals(reservedAfterEvent, nodeStatusTracker.getNodes(ReservationStatus.RESERVED));
		assertEquals(unreservedAfterEvent, nodeStatusTracker.getNodes(ReservationStatus.UNRESERVED));
	}
}
