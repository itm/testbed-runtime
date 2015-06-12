package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactoryImpl;
import eu.wisebed.api.v3.common.NodeUrn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static com.google.common.collect.Sets.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReservationEventBusImplTest {

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

	static {
		LINKS_RESERVED.put(GW1_N1, GW1_N2);
		LINKS_RESERVED.put(GW2_N1, GW2_N3);

		LINKS_UNRESERVED.put(GW1_N1, GW2_N2);
		LINKS_UNRESERVED.put(GW2_N2, GW2_N3);
	}

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
		reservationEventBus.startAsync().awaitRunning();
	}

	@After
	public void tearDown() throws Exception {
		reservationEventBus.stopAsync().awaitTerminated();
		reservationEventBus = null;
	}

	@Test
	public void testRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
		postRequestAndVerifyForwarded(
				MESSAGE_FACTORY.areNodesAliveRequest(Optional.of(RESERVATION_ID), Optional.empty(), RESERVED_NODES)
		);
	}

	@Test
	public void testRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
		postRequestAndVerifyException(
				MESSAGE_FACTORY.areNodesAliveRequest(Optional.of(RESERVATION_ID), Optional.empty(), UNRESERVED_NODES)
		);
		postRequestAndVerifyException(
				MESSAGE_FACTORY.areNodesAliveRequest(Optional.of(RESERVATION_ID), Optional.empty(), ALL_NODES)
		);
	}

	private void postRequestAndVerifyForwarded(final MessageLite request) {
		reservationEventBus.post(request);
		verify(eventBus).post(eq(request));
	}

	private void postRequestAndVerifyException(final MessageLite request) {
		try {
			reservationEventBus.post(request);
			fail("An IllegalArgumentException should have been thrown!");
		} catch (IllegalArgumentException expected) {
			// expected
		}
	}
}
