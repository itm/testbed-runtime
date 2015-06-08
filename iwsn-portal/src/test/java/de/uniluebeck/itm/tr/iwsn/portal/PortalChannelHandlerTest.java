package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.externalplugins.ExternalPluginService;
import de.uniluebeck.itm.util.logging.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PortalChannelHandlerTest {

	private static final Random RANDOM = new Random();
	private static final NodeUrn GATEWAY1_NODE1 = new NodeUrn("urn:unit-test:0x0011");
	private static final NodeUrn GATEWAY2_NODE1 = new NodeUrn("urn:unit-test:0x0021");
	private static final NodeUrn GATEWAY2_NODE2 = new NodeUrn("urn:unit-test:0x0022");
	private static final NodeUrn GATEWAY3_NODE1_UNCONNECTED = new NodeUrn("urn:unit-test:0x0031");
	private static final HashSet<NodeUrn> GATEWAY1_NODE_URNS = newHashSet(
			GATEWAY1_NODE1
	);
	private static final HashSet<NodeUrn> GATEWAY2_NODE_URNS = newHashSet(
			GATEWAY2_NODE1,
			GATEWAY2_NODE2
	);
	private static final HashSet<NodeUrn> GATEWAY3_NODE_URNS = newHashSet(
			GATEWAY3_NODE1_UNCONNECTED
	);
	private static final Iterable<NodeUrn> ALL_CONNECTED_NODE_URNS = newHashSet(
			GATEWAY1_NODE1,
			GATEWAY2_NODE1,
			GATEWAY2_NODE2
	);
	private static final Multimap<NodeUrn, NodeUrn> LINKS_GW1 = HashMultimap.create();
	private static final Multimap<NodeUrn, NodeUrn> LINKS_GW2 = HashMultimap.create();
	private static final Multimap<NodeUrn, NodeUrn> LINKS = HashMultimap.create();
	private static final Iterable<ChannelHandlerConfiguration> CHANNEL_HANDLER_CONFIGS =
			newArrayList(
					ChannelHandlerConfiguration.newBuilder()
							.setName("n1")
							.addConfiguration(ChannelHandlerConfiguration.KeyValuePair
											.newBuilder()
											.setKey("k1")
											.setValue("v1")
							).build(),
					ChannelHandlerConfiguration.newBuilder()
							.setName("n2")
							.addConfiguration(ChannelHandlerConfiguration.KeyValuePair
											.newBuilder()
											.setKey("n2k1")
											.setValue("n2v1")
							)
							.addConfiguration(ChannelHandlerConfiguration.KeyValuePair
											.newBuilder()
											.setKey("n2k2")
											.setValue("n2v2")
							).build()
			);
	private static final String RESERVATION_ID = "" + RANDOM.nextLong();

	static {
		Logging.setLoggingDefaults();
	}

	static {

		LINKS_GW1.put(GATEWAY1_NODE1, GATEWAY2_NODE1);

		LINKS_GW2.put(GATEWAY2_NODE1, GATEWAY1_NODE1);
		LINKS_GW2.put(GATEWAY2_NODE1, GATEWAY2_NODE2);

		LINKS.putAll(LINKS_GW1);
		LINKS.putAll(LINKS_GW2);
	}

	@Mock
	private PortalEventBus portalEventBus;

	@Mock
	private Channel gateway1Channel;

	@Mock
	private Channel gateway2Channel;

	@Mock
	private Channel gateway3Channel;

	@Mock
	private ChannelHandlerContext gateway1Context;

	@Mock
	private ChannelHandlerContext gateway2Context;

	@Mock
	private ChannelHandlerContext gateway3Context;

	@Mock
	private ChannelHandlerContext portalContext;

	@Mock
	private java.net.SocketAddress gateway1Address;

	@Mock
	private java.net.SocketAddress gateway2Address;

	@Mock
	private java.net.SocketAddress gateway3Address;

	@Mock
	private ExternalPluginService externalPluginService;

	@Mock
	private IdProvider idProvider;

	private MessageFactory messageFactory;

	private PortalChannelHandler portalChannelHandler;

	@Before
	public void setUp() throws Exception {

		messageFactory = new MessageFactoryImpl(new IncrementalIdProvider(), new UnixTimestampProvider());

		setUpGatewayAndChannelMockBehavior();

		portalChannelHandler = new PortalChannelHandler(portalEventBus, messageFactory);

		portalChannelHandler.channelConnected(
				gateway1Context,
				new UpstreamChannelStateEvent(gateway1Channel, ChannelState.CONNECTED, true)
		);
		portalChannelHandler.channelConnected(
				gateway3Context,
				new UpstreamChannelStateEvent(gateway2Channel, ChannelState.CONNECTED, true)
		);
		portalChannelHandler.channelConnected(
				gateway3Context,
				new UpstreamChannelStateEvent(gateway3Channel, ChannelState.CONNECTED, true)
		);

		final Channel channel1 = mock(Channel.class);
		final SocketAddress socketAddress1 = mock(SocketAddress.class);

		when(gateway1Context.getChannel()).thenReturn(channel1);
		when(channel1.getRemoteAddress()).thenReturn(socketAddress1);
		when(socketAddress1.toString()).thenReturn("192.168.0.1");

		portalChannelHandler.messageReceived(gateway1Context, new UpstreamMessageEvent(
				gateway1Channel,
				MessageHeaderPair.fromUnwrapped(messageFactory.devicesAttachedEvent(empty(), GATEWAY1_NODE1)),
				null
		));

		final Channel channel2 = mock(Channel.class);
		final SocketAddress socketAddress2 = mock(SocketAddress.class);

		when(gateway2Context.getChannel()).thenReturn(channel2);
		when(channel2.getRemoteAddress()).thenReturn(socketAddress2);
		when(socketAddress2.toString()).thenReturn("192.168.0.2");

		portalChannelHandler.messageReceived(gateway2Context, new UpstreamMessageEvent(
				gateway2Channel,
				MessageHeaderPair.fromUnwrapped(messageFactory.devicesAttachedEvent(empty(), GATEWAY2_NODE1, GATEWAY2_NODE2)),
				null
		));

		reset(gateway1Context);
		reset(gateway2Context);
		reset(gateway3Context);

		setUpGatewayAndChannelMockBehavior();
	}

	@Test
	public void testIfRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();
		Optional<Long> now = of(DateTime.now().getMillis());

		AreNodesAliveRequest request = messageFactory.areNodesAliveRequest(
				of(RESERVATION_ID),
				requestId,
				now,
				ALL_CONNECTED_NODE_URNS
		);

		portalChannelHandler.on(request);

		AreNodesAliveRequest expected1 = messageFactory.areNodesAliveRequest(
				of(RESERVATION_ID),
				requestId,
				now,
				GATEWAY1_NODE_URNS
		);

		AreNodesAliveRequest expected2 = messageFactory.areNodesAliveRequest(
				of(RESERVATION_ID),
				requestId,
				now,
				GATEWAY2_NODE_URNS
		);

		assertEqualHeaders(expected1, (AreNodesAliveRequest) verifyAndCaptureMessage(gateway1Context, gateway1Channel).message);
		assertEqualHeaders(expected2, (AreNodesAliveRequest) verifyAndCaptureMessage(gateway2Context, gateway2Channel).message);
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	private void assertEqualHeaders(AreNodesAliveRequest expectedRequest, AreNodesAliveRequest actualRequest) {
		Header expected = expectedRequest.getHeader();
		Header actual = actualRequest.getHeader();
		assertEquals(expected.getBroadcast(), actual.getBroadcast());
		assertEquals(expected.getCorrelationId(), actual.getCorrelationId());
		assertEquals(expected.getDownstream(), actual.getDownstream());
		assertEquals(newHashSet(expected.getNodeUrnsList()), newHashSet(actual.getNodeUrnsList()));
		assertEquals(expected.hasSerializedReservationKey(), actual.hasSerializedReservationKey());
		if (expected.hasSerializedReservationKey()) {
			assertEquals(expected.getSerializedReservationKey(), actual.getSerializedReservationKey());
		}
		assertEquals(expected.getTimestamp(), actual.getTimestamp());
		assertEquals(expected.getType(), actual.getType());
		assertEquals(expected.getUpstream(), actual.getUpstream());
	}

	@Test
	public void testRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		portalChannelHandler.on(
				messageFactory.areNodesAliveRequest(of(RESERVATION_ID), requestId, empty(), GATEWAY3_NODE_URNS)
		);
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, 0, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testIfUpstreamEventsAreAcknowledged() throws Exception {
		de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent event =
				messageFactory.upstreamMessageEvent(of(System.currentTimeMillis()), GATEWAY1_NODE1, new byte[]{1, 2, 3});
		portalChannelHandler.messageReceived(
				gateway1Context,
				new UpstreamMessageEvent(gateway1Channel, MessageHeaderPair.fromUnwrapped(event), gateway1Address)
		);

		EventAck eventAck = (EventAck) verifyAndCaptureMessage(gateway1Context, gateway1Channel).message;
		assertEquals(event.getHeader().getCorrelationId(), eventAck.getHeader().getCorrelationId());
	}

	private void verifyThatNodeUnconnectedResponseIsPostedBack(final long expectedRequestId,
															   final int expectedStatusCode,
															   final NodeUrn expectedNodeUrn,
															   @Nullable final String expectedErrorMessage) {
		final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
		verify(portalEventBus).post(captor.capture());
		final Response capturedResponse = captor.getValue();

		assertEquals(expectedRequestId, capturedResponse.getHeader().getCorrelationId());
		assertEquals(expectedStatusCode, capturedResponse.getStatusCode());
		assertTrue(capturedResponse.getHeader().getNodeUrnsCount() == 1);
		assertEquals(expectedNodeUrn, new NodeUrn(capturedResponse.getHeader().getNodeUrns(0)));
		assertEquals(expectedErrorMessage, capturedResponse.getErrorMessage());
	}

	private MessageHeaderPair verifyAndCaptureMessage(final ChannelHandlerContext context, final Channel channel) {

		ArgumentCaptor<DownstreamMessageEvent> captor = ArgumentCaptor.forClass(DownstreamMessageEvent.class);
		verify(context).sendDownstream(captor.capture());
		DownstreamMessageEvent captured = captor.getValue();
		assertSame(channel, captured.getChannel());
		return (MessageHeaderPair) captured.getMessage();
	}

	private void setUpGatewayAndChannelMockBehavior() {

		when(gateway1Channel.getId()).thenReturn(1);
		when(gateway2Channel.getId()).thenReturn(2);
		when(gateway3Channel.getId()).thenReturn(3);

		when(gateway1Channel.getCloseFuture()).thenReturn(mock(ChannelFuture.class));
		when(gateway2Channel.getCloseFuture()).thenReturn(mock(ChannelFuture.class));
		when(gateway3Channel.getCloseFuture()).thenReturn(mock(ChannelFuture.class));

		when(gateway1Context.getChannel()).thenReturn(gateway1Channel);
		when(gateway2Context.getChannel()).thenReturn(gateway2Channel);
		when(gateway3Context.getChannel()).thenReturn(gateway3Channel);

		when(gateway1Channel.getRemoteAddress()).thenReturn(gateway1Address);
		when(gateway2Channel.getRemoteAddress()).thenReturn(gateway2Address);
		when(gateway3Channel.getRemoteAddress()).thenReturn(gateway3Address);

		when(gateway1Address.toString()).thenReturn("192.168.0.1");
		when(gateway2Address.toString()).thenReturn("192.168.0.2");
		when(gateway3Address.toString()).thenReturn("192.168.0.3");
	}
}
