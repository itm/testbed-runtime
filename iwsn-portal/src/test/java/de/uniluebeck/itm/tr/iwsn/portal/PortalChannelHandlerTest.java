package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.SetChannelPipelinesRequest;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PortalChannelHandlerTest {

	static {
		Logging.setLoggingDefaults();
	}

	private static final Random RANDOM = new Random();

	private static final NodeUrn GATEWAY1_NODE1 = new NodeUrn("urn:unit-test:0x0011");

	private static final NodeUrn GATEWAY2_NODE1 = new NodeUrn("urn:unit-test:0x0021");

	private static final NodeUrn GATEWAY2_NODE2 = new NodeUrn("urn:unit-test:0x0022");

	private static final HashSet<NodeUrn> GATEWAY1_NODE_URN_STRINGS = newHashSet(
			GATEWAY1_NODE1
	);

	private static final HashSet<NodeUrn> GATEWAY2_NODE_URN_STRINGS = newHashSet(
			GATEWAY2_NODE1,
			GATEWAY2_NODE2
	);

	private static final Iterable<NodeUrn> ALL_NODE_URNS = newHashSet(
			GATEWAY1_NODE1,
			GATEWAY2_NODE1,
			GATEWAY2_NODE2
	);

	private static final Multimap<NodeUrn, NodeUrn> LINKS_GW1 = HashMultimap.create();

	private static final Multimap<NodeUrn, NodeUrn> LINKS_GW2 = HashMultimap.create();

	private static final Multimap<NodeUrn, NodeUrn> LINKS = HashMultimap.create();

	static {

		LINKS_GW1.put(GATEWAY1_NODE1, GATEWAY2_NODE1);

		LINKS_GW2.put(GATEWAY2_NODE1, GATEWAY1_NODE1);
		LINKS_GW2.put(GATEWAY2_NODE1, GATEWAY2_NODE2);

		LINKS.putAll(LINKS_GW1);
		LINKS.putAll(LINKS_GW2);
	}

	private static final Iterable<SetChannelPipelinesRequest.ChannelHandlerConfiguration> CHANNEL_HANDLER_CONFIGS =
			newArrayList(
					SetChannelPipelinesRequest.ChannelHandlerConfiguration.newBuilder()
							.setName("n1")
							.addConfiguration(
									SetChannelPipelinesRequest.ChannelHandlerConfiguration.KeyValuePair.newBuilder()
											.setKey("k1").setValue("v1")
							).build(),
					SetChannelPipelinesRequest.ChannelHandlerConfiguration.newBuilder()
							.setName("n2")
							.addConfiguration(
									SetChannelPipelinesRequest.ChannelHandlerConfiguration.KeyValuePair.newBuilder()
											.setKey("n2k1").setValue("n2v1")
							)
							.addConfiguration(
									SetChannelPipelinesRequest.ChannelHandlerConfiguration.KeyValuePair.newBuilder()
											.setKey("n2k2").setValue("n2v2")
							).build()
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

	@Test
	public void testIfAreNodesAliveRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newAreNodesAliveRequest(requestId, ALL_NODE_URNS));

		final Message expectedMessage1 = newAreNodesAliveRequestMessage(requestId, GATEWAY1_NODE_URN_STRINGS);
		final Message expectedMessage2 = newAreNodesAliveRequestMessage(requestId, GATEWAY2_NODE_URN_STRINGS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfAreNodesConnectedRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newAreNodesConnectedRequest(requestId, ALL_NODE_URNS));

		final Message expectedMessage1 = newAreNodesConnectedRequestMessage(requestId, GATEWAY1_NODE_URN_STRINGS);
		final Message expectedMessage2 = newAreNodesConnectedRequestMessage(requestId, GATEWAY2_NODE_URN_STRINGS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfDisableNodesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newDisableNodesRequest(requestId, ALL_NODE_URNS));

		final Message expectedMessage1 = newDisableNodesRequestMessage(requestId, GATEWAY1_NODE_URN_STRINGS);
		final Message expectedMessage2 = newDisableNodesRequestMessage(requestId, GATEWAY2_NODE_URN_STRINGS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfEnableNodesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newEnableNodesRequest(requestId, ALL_NODE_URNS));

		final Message expectedMessage1 = newEnableNodesRequestMessage(requestId, GATEWAY1_NODE_URN_STRINGS);
		final Message expectedMessage2 = newEnableNodesRequestMessage(requestId, GATEWAY2_NODE_URN_STRINGS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfResetNodesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newResetNodesRequest(requestId, ALL_NODE_URNS));

		final Message expectedMessage1 = newResetNodesRequestMessage(requestId, GATEWAY1_NODE_URN_STRINGS);
		final Message expectedMessage2 = newResetNodesRequestMessage(requestId, GATEWAY2_NODE_URN_STRINGS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfSendDownstreamMessageRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();
		final byte[] bytes = "Hello, World!".getBytes(Charset.defaultCharset());

		portalChannelHandler.onRequest(newSendDownstreamMessageRequest(requestId, ALL_NODE_URNS, bytes));

		final Message expectedMessage1 =
				newSendDownstreamMessageRequestMessage(requestId, GATEWAY1_NODE_URN_STRINGS, bytes);
		final Message expectedMessage2 =
				newSendDownstreamMessageRequestMessage(requestId, GATEWAY2_NODE_URN_STRINGS, bytes);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfFlashImagesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();
		final byte[] imageBytes = "Hello, World!".getBytes(Charset.defaultCharset());

		portalChannelHandler.onRequest(newFlashImagesRequest(requestId, ALL_NODE_URNS, imageBytes));

		final Message expectedMessage1 = newFlashImagesRequestMessage(requestId, GATEWAY1_NODE_URN_STRINGS, imageBytes);
		final Message expectedMessage2 = newFlashImagesRequestMessage(requestId, GATEWAY2_NODE_URN_STRINGS, imageBytes);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfDestroyVirtualLinksRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newDisableVirtualLinksRequest(requestId, LINKS));

		final Message expectedMessage1 = newDisableVirtualLinksRequestMessage(requestId, LINKS_GW1);
		final Message expectedMessage2 = newDisableVirtualLinksRequestMessage(requestId, LINKS_GW2);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfEnableVirtualLinksRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newEnableVirtualLinksRequest(requestId, LINKS));

		final Message expectedMessage1 = newEnableVirtualLinksRequestMessage(requestId, LINKS_GW1);
		final Message expectedMessage2 = newEnableVirtualLinksRequestMessage(requestId, LINKS_GW2);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfDisablePhysicalLinksRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newDisablePhysicalLinksRequest(requestId, LINKS));

		final Message expectedMessage1 = newDisablePhysicalLinksRequestMessage(requestId, LINKS_GW1);
		final Message expectedMessage2 = newDisablePhysicalLinksRequestMessage(requestId, LINKS_GW2);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfEnablePhysicalLinksRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newEnablePhysicalLinksRequest(requestId, LINKS));

		final Message expectedMessage1 = newEnablePhysicalLinksRequestMessage(requestId, LINKS_GW1);
		final Message expectedMessage2 = newEnablePhysicalLinksRequestMessage(requestId, LINKS_GW2);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfSetChannelPipelinesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(
				newSetChannelPipelinesRequest(requestId, ALL_NODE_URNS, CHANNEL_HANDLER_CONFIGS)
		);

		final Message expectedMessage1 =
				newSetChannelPipelinesRequestMessage(requestId, GATEWAY1_NODE_URN_STRINGS, CHANNEL_HANDLER_CONFIGS);
		final Message expectedMessage2 =
				newSetChannelPipelinesRequestMessage(requestId, GATEWAY2_NODE_URN_STRINGS, CHANNEL_HANDLER_CONFIGS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	private Message verifyAndCaptureMessage(final ChannelHandlerContext context, final Channel channel) {

		ArgumentCaptor<DownstreamMessageEvent> captor = ArgumentCaptor.forClass(DownstreamMessageEvent.class);
		verify(context).sendDownstream(captor.capture());
		assertSame(channel, captor.getValue().getChannel());
		assertSame(null, captor.getValue().getRemoteAddress());

		return (Message) captor.getValue().getMessage();
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
}
