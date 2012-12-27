package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Random;

import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newAreNodesAliveRequest;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newAreNodesConnectedRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

/**
 * DESTROY_VIRTUAL_LINK
 * DISABLE_NODE
 * DISABLE_PHYSICAL_LINK
 * ENABLE_NODE
 * ENABLE_PHYSICAL_LINK
 * FLASH_PROGRAMS
 * RESET_NODES
 * SEND_DOWNSTREAM_MESSAGE
 * SET_CHANNEL_PIPELINE
 * SET_DEFAULT_CHANNEL_PIPELINE
 * SET_VIRTUAL_LINK
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalChannelHandlerTest {

	static {
		Logging.setLoggingDefaults();
	}

	private static final Random RANDOM = new Random();

	private static final NodeUrn GATEWAY1_NODE1 = new NodeUrn("urn:unittest:0x0011");

	private static final HashSet<String> GATEWAY1_NODE_URN_STRINGS = newHashSet(GATEWAY1_NODE1.toString());

	private static final NodeUrn GATEWAY2_NODE1 = new NodeUrn("urn:unittest:0x0021");

	private static final NodeUrn GATEWAY2_NODE2 = new NodeUrn("urn:unittest:0x0022");

	private static final HashSet<String> GATEWAY2_NODE_URN_STRINGS = newHashSet(
			GATEWAY2_NODE1.toString(),
			GATEWAY2_NODE2.toString()
	);

	private static final Iterable<? extends String> ALL_NODE_URNS = newHashSet(
			GATEWAY1_NODE1.toString(),
			GATEWAY2_NODE1.toString(),
			GATEWAY2_NODE2.toString()
	);

	@Mock
	private PortalEventBus portalEventBus;

	private PortalChannelHandler portalChannelHandler;

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

	@Before
	public void setUp() throws Exception {

		setUpGatewayAndChannelMockBehavior();

		portalChannelHandler = new PortalChannelHandler(portalEventBus);
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

		portalChannelHandler.messageReceived(gateway1Context, new UpstreamMessageEvent(
				gateway1Channel,
				Message.newBuilder()
						.setType(Message.Type.EVENT)
						.setEvent(
								Event.newBuilder()
										.setType(Event.Type.DEVICES_ATTACHED)
										.setEventId(RANDOM.nextLong())
										.setDevicesAttachedEvent(
												DevicesAttachedEvent.newBuilder()
														.addNodeUrns(GATEWAY1_NODE1.toString())
														.setTimestamp(new DateTime().getMillis())
										)
						).build(),
				null
		)
		);

		portalChannelHandler.messageReceived(gateway2Context, new UpstreamMessageEvent(
				gateway2Channel,
				Message.newBuilder()
						.setType(Message.Type.EVENT)
						.setEvent(
								Event.newBuilder()
										.setType(Event.Type.DEVICES_ATTACHED)
										.setEventId(RANDOM.nextLong())
										.setDevicesAttachedEvent(
												DevicesAttachedEvent.newBuilder()
														.addNodeUrns(GATEWAY2_NODE1.toString())
														.addNodeUrns(GATEWAY2_NODE2.toString())
														.setTimestamp(new DateTime().getMillis())
										)
						).build(),
				null
		)
		);

		reset(gateway1Context);
		reset(gateway2Context);
		reset(gateway3Context);

		setUpGatewayAndChannelMockBehavior();
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
	}

	@Test
	public void testIfAreNodesAliveRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(Request.newBuilder()
				.setType(Request.Type.ARE_NODES_ALIVE)
				.setRequestId(requestId)
				.setAreNodesAliveRequest(AreNodesAliveRequest.newBuilder().addAllNodeUrns(ALL_NODE_URNS))
				.build()
		);

		final Message expectedMessage1 = newAreNodesAliveRequest(requestId, GATEWAY1_NODE_URN_STRINGS);
		final Message expectedMessage2 = newAreNodesAliveRequest(requestId, GATEWAY2_NODE_URN_STRINGS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
	}

	@Test
	public void testIfAreNodesConnectedRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(Request.newBuilder()
				.setType(Request.Type.ARE_NODES_CONNECTED)
				.setRequestId(requestId)
				.setAreNodesConnectedRequest(AreNodesConnectedRequest.newBuilder().addAllNodeUrns(ALL_NODE_URNS))
				.build()
		);

		final Message expectedMessage1 = newAreNodesConnectedRequest(requestId, GATEWAY1_NODE_URN_STRINGS);
		final Message expectedMessage2 = newAreNodesConnectedRequest(requestId, GATEWAY2_NODE_URN_STRINGS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
	}

	@Test
	public void testIfDisableNodesRequestIsCorrectlyDistributed() throws Exception {


	}

	private Message verifyAndCaptureMessage(final ChannelHandlerContext context, final Channel channel) {

		ArgumentCaptor<DownstreamMessageEvent> captor = ArgumentCaptor.forClass(DownstreamMessageEvent.class);
		verify(context).sendDownstream(captor.capture());
		assertSame(channel, captor.getValue().getChannel());
		assertSame(null, captor.getValue().getRemoteAddress());

		return (Message) captor.getValue().getMessage();
	}
}
