package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;//noinspection unchecked
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.*;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReservationEventBusImplTest {

    private static final NodeUrn GW1_N1 = new NodeUrn("urn:unit-test:gw1:0x0001");

    private static final NodeUrn GW1_N2 = new NodeUrn("urn:unit-test:gw1:0x0002");

    private static final NodeUrn GW1_N3 = new NodeUrn("urn:unit-test:gw1:0x0003");
    private static final Set<NodeUrn> GW1_NODES = newHashSet(GW1_N1, GW1_N2, GW1_N3);
    private static final NodeUrn GW2_N1 = new NodeUrn("urn:unit-test:gw2:0x0001");
    private static final NodeUrn GW2_N2 = new NodeUrn("urn:unit-test:gw2:0x0002");
    private static final NodeUrn GW2_N3 = new NodeUrn("urn:unit-test:gw2:0x0003");
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
    private static final Set<Reservation.Entry> ENTRIES = newHashSet(
            new Reservation.Entry(GW1_N1.getPrefix(), null, RESERVATION_ID, GW1_NODES, new Interval(0, 1), null),
            new Reservation.Entry(GW2_N1.getPrefix(), null, RESERVATION_ID, GW1_NODES, new Interval(0, 1), null)
    );
    private static final String OTHER_RESERVATION_ID = "" + RANDOM.nextLong();
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
    public void testAreNodesAliveRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newAreNodesAliveRequest(RESERVATION_ID, RANDOM.nextLong(), RESERVED_NODES));
    }

    @Test
    public void testAreNodesAliveRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(newAreNodesAliveRequest(RESERVATION_ID, RANDOM.nextLong(), UNRESERVED_NODES));
        postRequestAndVerifyException(newAreNodesAliveRequest(RESERVATION_ID, RANDOM.nextLong(), ALL_NODES));
    }

    @Test
    public void testAreNodesConnectedRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newAreNodesConnectedRequest(RESERVATION_ID, RANDOM.nextLong(), RESERVED_NODES));
    }

    @Test
    public void testAreNodesConnectedRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(newAreNodesConnectedRequest(RESERVATION_ID, RANDOM.nextLong(), UNRESERVED_NODES));
        postRequestAndVerifyException(newAreNodesConnectedRequest(RESERVATION_ID, RANDOM.nextLong(), ALL_NODES));
    }

    @Test
    public void testDisableNodesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newDisableNodesRequest(RESERVATION_ID, RANDOM.nextLong(), RESERVED_NODES));
    }

    @Test
    public void testDisableNodesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(newDisableNodesRequest(RESERVATION_ID, RANDOM.nextLong(), UNRESERVED_NODES));
        postRequestAndVerifyException(newDisableNodesRequest(RESERVATION_ID, RANDOM.nextLong(), ALL_NODES));
    }

    @Test
    public void testEnableNodesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newEnableNodesRequest(RESERVATION_ID, RANDOM.nextLong(), RESERVED_NODES));
    }

    @Test
    public void testEnableNodesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(newEnableNodesRequest(RESERVATION_ID, RANDOM.nextLong(), UNRESERVED_NODES));
        postRequestAndVerifyException(newEnableNodesRequest(RESERVATION_ID, RANDOM.nextLong(), ALL_NODES));
    }

    @Test
    public void testResetNodesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newResetNodesRequest(RESERVATION_ID, RANDOM.nextLong(), RESERVED_NODES));
    }

    @Test
    public void testResetNodesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(newResetNodesRequest(RESERVATION_ID, RANDOM.nextLong(), UNRESERVED_NODES));
        postRequestAndVerifyException(newResetNodesRequest(RESERVATION_ID, RANDOM.nextLong(), ALL_NODES));
    }

    @Test
    public void testDisableVirtualLinksRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newDisableVirtualLinksRequest(RESERVATION_ID, RANDOM.nextLong(), LINKS_RESERVED));
    }

    @Test
    public void testDisableVirtualLinksRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(
                newDisableVirtualLinksRequest(RESERVATION_ID, RANDOM.nextLong(), LINKS_UNRESERVED)
        );
    }

    @Test
    public void testEnableVirtualLinksRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newEnableVirtualLinksRequest(RESERVATION_ID, RANDOM.nextLong(), LINKS_RESERVED));
    }

    @Test
    public void testEnableVirtualLinksRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(newEnableVirtualLinksRequest(RESERVATION_ID, RANDOM.nextLong(), LINKS_UNRESERVED)
        );
    }

    @Test
    public void testDisablePhysicalLinksRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newDisablePhysicalLinksRequest(RESERVATION_ID, RANDOM.nextLong(), LINKS_RESERVED)
        );
    }

    @Test
    public void testDisablePhysicalLinksRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(
                newDisablePhysicalLinksRequest(RESERVATION_ID, RANDOM.nextLong(), LINKS_UNRESERVED)
        );
    }

    @Test
    public void testEnablePhysicalLinksRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newEnablePhysicalLinksRequest(RESERVATION_ID, RANDOM.nextLong(), LINKS_RESERVED));
    }

    @Test
    public void testEnablePhysicalLinksRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(
                newEnablePhysicalLinksRequest(RESERVATION_ID, RANDOM.nextLong(), LINKS_UNRESERVED)
        );
    }

    @Test
    public void testSendDownstreamMessageRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        final byte[] bytes = "hello".getBytes(Charset.defaultCharset());
        postRequestAndVerifyForwarded(
                newSendDownstreamMessageRequest(RESERVATION_ID, RANDOM.nextLong(), RESERVED_NODES, bytes)
        );
    }

    @Test
    public void testSendDownstreamMessageRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        final byte[] bytes = "hello".getBytes(Charset.defaultCharset());
        postRequestAndVerifyException(
                newSendDownstreamMessageRequest(RESERVATION_ID, RANDOM.nextLong(), UNRESERVED_NODES, bytes)
        );
        postRequestAndVerifyException(
                newSendDownstreamMessageRequest(RESERVATION_ID, RANDOM.nextLong(), ALL_NODES, bytes)
        );
    }

    @Test
    public void testFlashImagesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        final byte[] imageBytes = "hello".getBytes(Charset.defaultCharset());
        postRequestAndVerifyForwarded(
                newFlashImagesRequest(RESERVATION_ID, RANDOM.nextLong(), RESERVED_NODES, imageBytes)
        );
    }

    @Test
    public void testFlashImagesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        final byte[] imageBytes = "hello".getBytes(Charset.defaultCharset());
        postRequestAndVerifyException(
                newFlashImagesRequest(RESERVATION_ID, RANDOM.nextLong(), UNRESERVED_NODES, imageBytes)
        );
        postRequestAndVerifyException(newFlashImagesRequest(RESERVATION_ID, RANDOM.nextLong(), ALL_NODES, imageBytes));
    }

    @Test
    public void testSetChannelPipelinesRequestCanBePostedIfNodeUrnsArePartOfReservation() throws Exception {
        postRequestAndVerifyForwarded(newSetChannelPipelinesRequest(
                        RESERVATION_ID,
                        RANDOM.nextLong(),
                        RESERVED_NODES,
                        CHANNEL_HANDLER_CONFIGURATIONS
                )
        );
    }

    @Test
    public void testSetChannelPipelinesRequestCanNotBePostedIfNodeUrnsAreNotPartOfReservation() throws Exception {
        postRequestAndVerifyException(newSetChannelPipelinesRequest(
                        RESERVATION_ID,
                        RANDOM.nextLong(),
                        UNRESERVED_NODES,
                        CHANNEL_HANDLER_CONFIGURATIONS
                )
        );
        postRequestAndVerifyException(newSetChannelPipelinesRequest(
                        RESERVATION_ID,
                        RANDOM.nextLong(),
                        ALL_NODES,
                        CHANNEL_HANDLER_CONFIGURATIONS
                )
        );
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
