package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStore;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Random;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.union;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private ReservationEventBus reservationEventBus;

    @Mock
    private EventBusFactory eventBusFactory;

    @Mock
    private EventBus eventBus;

    @Mock
    private Reservation reservation;
    @Mock
    private ReservationManager reservationManager;
    @Mock
    private PortalEventStore portalEventStore;

    private PortalEventDispatcherImpl portalEventDispatcher;


    @Before
    public void setUp() throws Exception {
        when(reservation.getNodeUrns()).thenReturn(RESERVED_NODES);
        when(reservation.getSerializedKey()).thenReturn(RESERVATION_ID);
        when(reservation.getEventBus()).thenReturn(reservationEventBus);
        when(eventBusFactory.create(anyString())).thenReturn(eventBus);
        portalEventDispatcher = new PortalEventDispatcherImpl(portalEventBus, reservationManager, portalEventStore);
        portalEventBus.startAndWait();
    }


    @Test
    public void testAreNodesAliveRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        portalEventDispatcher.on
    }

    @After
    public void tearDown() throws Exception {
        reservationEventBus.stopAndWait();
        reservationEventBus = null;
    }

    private void dispatchRequestAndVerifyForwarded(final Request request) {
        reservationEventBus.post(request);
        verify(eventBus).post(eq(request));
    }

    private void dispatchRequestAndVerifyException(final Request request) {
        try {
            reservationEventBus.post(request);
            fail("An IllegalArgumentException should have been thrown!");
        } catch (IllegalArgumentException expected) {
        }
    }
}
