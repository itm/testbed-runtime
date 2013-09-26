package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
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

	static {

		LINKS_GW1.put(GATEWAY1_NODE1, GATEWAY2_NODE1);

		LINKS_GW2.put(GATEWAY2_NODE1, GATEWAY1_NODE1);
		LINKS_GW2.put(GATEWAY2_NODE1, GATEWAY2_NODE2);

		LINKS.putAll(LINKS_GW1);
		LINKS.putAll(LINKS_GW2);
	}

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

	@Mock
	private ExternalPluginService externalPluginService;

	@Before
	public void setUp() throws Exception {

		setUpGatewayAndChannelMockBehavior();

		portalChannelHandler = new PortalChannelHandler(portalEventBus, externalPluginService);
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
						.setEvent(Event.newBuilder()
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
						.setEvent(Event.newBuilder()
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

		portalChannelHandler.onRequest(newAreNodesAliveRequest(RESERVATION_ID, requestId, ALL_CONNECTED_NODE_URNS));

		final Message expectedMessage1 =
				newAreNodesAliveRequestMessage(RESERVATION_ID, requestId, GATEWAY1_NODE_URNS);
		final Message expectedMessage2 =
				newAreNodesAliveRequestMessage(RESERVATION_ID, requestId, GATEWAY2_NODE_URNS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfAreNodesConnectedRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newAreNodesConnectedRequest(RESERVATION_ID, requestId, ALL_CONNECTED_NODE_URNS));

		final Message expectedMessage1 =
				newAreNodesConnectedRequestMessage(RESERVATION_ID, requestId, GATEWAY1_NODE_URNS);
		final Message expectedMessage2 =
				newAreNodesConnectedRequestMessage(RESERVATION_ID, requestId, GATEWAY2_NODE_URNS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfDisableNodesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newDisableNodesRequest(RESERVATION_ID, requestId, ALL_CONNECTED_NODE_URNS));

		final Message expectedMessage1 =
				newDisableNodesRequestMessage(RESERVATION_ID, requestId, GATEWAY1_NODE_URNS);
		final Message expectedMessage2 =
				newDisableNodesRequestMessage(RESERVATION_ID, requestId, GATEWAY2_NODE_URNS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfEnableNodesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newEnableNodesRequest(RESERVATION_ID, requestId, ALL_CONNECTED_NODE_URNS));

		final Message expectedMessage1 =
				newEnableNodesRequestMessage(RESERVATION_ID, requestId, GATEWAY1_NODE_URNS);
		final Message expectedMessage2 =
				newEnableNodesRequestMessage(RESERVATION_ID, requestId, GATEWAY2_NODE_URNS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfResetNodesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newResetNodesRequest(RESERVATION_ID, requestId, ALL_CONNECTED_NODE_URNS));

		final Message expectedMessage1 =
				newResetNodesRequestMessage(RESERVATION_ID, requestId, GATEWAY1_NODE_URNS);
		final Message expectedMessage2 =
				newResetNodesRequestMessage(RESERVATION_ID, requestId, GATEWAY2_NODE_URNS);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfSendDownstreamMessageRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();
		final byte[] bytes = "Hello, World!".getBytes(Charset.defaultCharset());

		portalChannelHandler.onRequest(newSendDownstreamMessageRequest(RESERVATION_ID, requestId,
				ALL_CONNECTED_NODE_URNS, bytes
		)
		);

		final Message expectedMessage1 =
				newSendDownstreamMessageRequestMessage(RESERVATION_ID, requestId, GATEWAY1_NODE_URNS, bytes);
		final Message expectedMessage2 =
				newSendDownstreamMessageRequestMessage(RESERVATION_ID, requestId, GATEWAY2_NODE_URNS, bytes);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfFlashImagesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();
		final byte[] imageBytes = "Hello, World!".getBytes(Charset.defaultCharset());

		portalChannelHandler
				.onRequest(newFlashImagesRequest(RESERVATION_ID, requestId, ALL_CONNECTED_NODE_URNS, imageBytes));

		final Message expectedMessage1 =
				newFlashImagesRequestMessage(RESERVATION_ID, requestId, GATEWAY1_NODE_URNS, imageBytes);
		final Message expectedMessage2 =
				newFlashImagesRequestMessage(RESERVATION_ID, requestId, GATEWAY2_NODE_URNS, imageBytes);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfDestroyVirtualLinksRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newDisableVirtualLinksRequest(RESERVATION_ID, requestId, LINKS));

		final Message expectedMessage1 = newDisableVirtualLinksRequestMessage(RESERVATION_ID, requestId, LINKS_GW1);
		final Message expectedMessage2 = newDisableVirtualLinksRequestMessage(RESERVATION_ID, requestId, LINKS_GW2);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfEnableVirtualLinksRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newEnableVirtualLinksRequest(RESERVATION_ID, requestId, LINKS));

		final Message expectedMessage1 = newEnableVirtualLinksRequestMessage(RESERVATION_ID, requestId, LINKS_GW1);
		final Message expectedMessage2 = newEnableVirtualLinksRequestMessage(RESERVATION_ID, requestId, LINKS_GW2);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfDisablePhysicalLinksRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newDisablePhysicalLinksRequest(RESERVATION_ID, requestId, LINKS));

		final Message expectedMessage1 = newDisablePhysicalLinksRequestMessage(RESERVATION_ID, requestId, LINKS_GW1);
		final Message expectedMessage2 = newDisablePhysicalLinksRequestMessage(RESERVATION_ID, requestId, LINKS_GW2);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfEnablePhysicalLinksRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(newEnablePhysicalLinksRequest(RESERVATION_ID, requestId, LINKS));

		final Message expectedMessage1 = newEnablePhysicalLinksRequestMessage(RESERVATION_ID, requestId, LINKS_GW1);
		final Message expectedMessage2 = newEnablePhysicalLinksRequestMessage(RESERVATION_ID, requestId, LINKS_GW2);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testIfSetChannelPipelinesRequestIsCorrectlyDistributed() throws Exception {

		final long requestId = RANDOM.nextLong();

		portalChannelHandler.onRequest(
				newSetChannelPipelinesRequest(RESERVATION_ID, requestId, ALL_CONNECTED_NODE_URNS,
						CHANNEL_HANDLER_CONFIGS
				)
		);

		final Message expectedMessage1 =
				newSetChannelPipelinesRequestMessage(RESERVATION_ID, requestId, GATEWAY1_NODE_URNS,
						CHANNEL_HANDLER_CONFIGS
				);
		final Message expectedMessage2 =
				newSetChannelPipelinesRequestMessage(RESERVATION_ID, requestId, GATEWAY2_NODE_URNS,
						CHANNEL_HANDLER_CONFIGS
				);

		assertEquals(expectedMessage1, verifyAndCaptureMessage(gateway1Context, gateway1Channel));
		assertEquals(expectedMessage2, verifyAndCaptureMessage(gateway2Context, gateway2Channel));
		verify(gateway3Context, never()).sendDownstream(Matchers.<ChannelEvent>any());
	}

	@Test
	public void testAreNodesAliveRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		portalChannelHandler.onRequest(newAreNodesAliveRequest(RESERVATION_ID, requestId, GATEWAY3_NODE_URNS));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, 0, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testAreNodesConnectedRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		portalChannelHandler.onRequest(newAreNodesConnectedRequest(RESERVATION_ID, requestId, GATEWAY3_NODE_URNS));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, 0, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testDisableNodesConnectedRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		portalChannelHandler.onRequest(newDisableNodesRequest(RESERVATION_ID, requestId, GATEWAY3_NODE_URNS));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testDisablePhysicalLinksRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		final HashMultimap<NodeUrn, NodeUrn> links = HashMultimap.create();
		links.put(GATEWAY3_NODE1_UNCONNECTED, GATEWAY1_NODE1);
		portalChannelHandler.onRequest(newDisablePhysicalLinksRequest(RESERVATION_ID, requestId, links));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testDisableVirtualLinksRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		final HashMultimap<NodeUrn, NodeUrn> links = HashMultimap.create();
		links.put(GATEWAY3_NODE1_UNCONNECTED, GATEWAY1_NODE1);
		portalChannelHandler.onRequest(newDisableVirtualLinksRequest(RESERVATION_ID, requestId, links));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testEnableNodesRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		portalChannelHandler.onRequest(newEnableNodesRequest(RESERVATION_ID, requestId, GATEWAY3_NODE_URNS));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testEnablePhysicalLinksRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		final HashMultimap<NodeUrn, NodeUrn> links = HashMultimap.create();
		links.put(GATEWAY3_NODE1_UNCONNECTED, GATEWAY1_NODE1);
		portalChannelHandler.onRequest(newEnablePhysicalLinksRequest(RESERVATION_ID, requestId, links));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testEnableVirtualLinksRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		final HashMultimap<NodeUrn, NodeUrn> links = HashMultimap.create();
		links.put(GATEWAY3_NODE1_UNCONNECTED, GATEWAY1_NODE1);
		portalChannelHandler.onRequest(newEnableVirtualLinksRequest(RESERVATION_ID, requestId, links));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testResetRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		portalChannelHandler.onRequest(newResetNodesRequest(RESERVATION_ID, requestId, GATEWAY3_NODE_URNS));
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testSendDownstreamMessageRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		portalChannelHandler.onRequest(newSendDownstreamMessageRequest(RESERVATION_ID, requestId, GATEWAY3_NODE_URNS,
				new byte[]{1, 2, 3}
		)
		);
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	@Test
	public void testSetChannelPipelinesRequestToUnconnectedNode() throws Exception {
		final long requestId = RANDOM.nextLong();
		reset(portalEventBus);
		final List<ChannelHandlerConfiguration> configs = Lists.newArrayList();
		portalChannelHandler.onRequest(
				newSetChannelPipelinesRequest(RESERVATION_ID, requestId, GATEWAY3_NODE_URNS, configs)
		);
		verifyThatNodeUnconnectedResponseIsPostedBack(requestId, -1, GATEWAY3_NODE1_UNCONNECTED,
				"Node is not connected"
		);
	}

	private void verifyThatNodeUnconnectedResponseIsPostedBack(final long expectedRequestId,
															   final int expectedStatusCode,
															   final NodeUrn expectedNodeUrn,
															   @Nullable final String expectedErrorMessage) {
		final ArgumentCaptor<SingleNodeResponse> captor = ArgumentCaptor.forClass(SingleNodeResponse.class);
		verify(portalEventBus).post(captor.capture());
		final SingleNodeResponse capturedResponse = captor.getValue();

		assertEquals(expectedRequestId, capturedResponse.getRequestId());
		assertEquals(expectedStatusCode, capturedResponse.getStatusCode());
		assertEquals(expectedNodeUrn, new NodeUrn(capturedResponse.getNodeUrn()));
		assertEquals(expectedErrorMessage, capturedResponse.getErrorMessage());
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
