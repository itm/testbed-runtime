package de.uniluebeck.itm.tr.iwsn.gateway;

import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.gateway.eventqueue.UpstreamMessageQueue;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GatewayChannelHandlerTest {

	private static final MessageFactory MESSAGE_FACTORY = new MessageFactoryImpl(new IncrementalIdProvider(), new UnixTimestampProvider());

	private static final NodeUrn NODE_URN_1 = new NodeUrn("urn:unit:test:0x0001");
	private static final NodeUrn NODE_URN_2 = new NodeUrn("urn:unit:test:0x0002");

	@Mock
	private GatewayEventBus eventBus;

	@Mock
	private DeviceManager deviceManager;

	@Mock
	private UpstreamMessageQueue queue;

	@Mock
	private ChannelHandlerContext ctx;

	@Mock
	private Channel channel;

	private GatewayChannelHandler handler;

	@Before
	public void setUp() throws Exception {

		when(ctx.getChannel()).thenReturn(channel);
		when(queue.isRunning()).thenReturn(true);
		when(queue.startAsync()).thenReturn(queue);
		when(queue.stopAsync()).thenReturn(queue);

		handler = new GatewayChannelHandler(eventBus, deviceManager, queue, MESSAGE_FACTORY);
	}

	@Test
	public void testStartsQueueAndRegistersOnEventBusOnStart() throws Exception {
		handler.start();
		InOrder inOrder = inOrder(queue, eventBus);
		inOrder.verify(queue).startAsync();
		inOrder.verify(eventBus).register(same(handler));
	}

	@Test
	public void testUnregistersFromEventBusAndStopsQueueOnStop() throws Exception {
		handler.stop();
		InOrder inOrder = inOrder(queue, eventBus);
		inOrder.verify(eventBus).unregister(same(handler));
		inOrder.verify(queue).stopAsync();
	}

	@Test
	public void testQueuesEventsWhenNotConnected() throws Exception {

		handler.start();
		testQueuesEventsWhenNotConnectedInternal();
	}

	private void testQueuesEventsWhenNotConnectedInternal() {
		UpstreamMessageEvent event = createUpstreamMessageEvent();
		handler.onEvent(event);
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(queue).enqueue(captor.capture());
		assertSame(event, captor.getValue().getUpstreamMessageEvent());
	}

	private UpstreamMessageEvent createUpstreamMessageEvent() {
		return MESSAGE_FACTORY.upstreamMessageEvent(Optional.empty(), NODE_URN_1, new byte[]{1, 2, 3});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSendsGatewayConnectedEventReplaysQueueAndSendsDevicesAttachedEventsOnConnection() throws Exception {

		// set up scenario: two messages in the queue, two devices currently connected
		handler.start();
		ChannelFuture future = mock(ChannelFuture.class);
		when(channel.write(any())).thenReturn(future);
		when(future.isSuccess()).thenReturn(true);

		Message replayMsg1 = MessageWrapper.wrap(MESSAGE_FACTORY.devicesAttachedEvent(empty(), NODE_URN_1));
		Message replayMsg2 = MessageWrapper.wrap(MESSAGE_FACTORY.devicesAttachedEvent(empty(), NODE_URN_2));
		when(queue.dequeue()).thenReturn(of(replayMsg1), of(replayMsg2), empty());

		when(deviceManager.getConnectedNodeUrns()).thenReturn(newHashSet(NODE_URN_1, NODE_URN_2));

		// trigger any actions
		simulateConnect();

		// verify in order
		InOrder inOrder = inOrder(channel, queue, deviceManager);

		// first, the handler should send a GatewayConnectedEvent
		ArgumentCaptor<Message> gatewayConnectedCaptor = ArgumentCaptor.forClass(Message.class);
		inOrder.verify(channel).write(gatewayConnectedCaptor.capture());
		assertEquals(MessageType.EVENT_GATEWAY_CONNECTED, gatewayConnectedCaptor.getValue().getType());
		assertNotNull(gatewayConnectedCaptor.getValue().getGatewayConnectedEvent());

		// second, it should replay everything in the queue
		inOrder.verify(channel).write(same(replayMsg1));
		inOrder.verify(channel).write(same(replayMsg2));

		// third, it should send DevicesAttachedEvents for all attached devices
		inOrder.verify(deviceManager).getConnectedNodeUrns();
		ArgumentCaptor<Message> devicesAttachedCaptor = ArgumentCaptor.forClass(Message.class);
		inOrder.verify(channel).write(devicesAttachedCaptor.capture());

		Message captured = devicesAttachedCaptor.getValue();
		assertEquals(MessageType.EVENT_DEVICES_ATTACHED, captured.getType());
		assertNotNull(captured.getDevicesAttachedEvent());
		assertTrue(captured.getDevicesAttachedEvent().getHeader().getNodeUrnsList().size() == 2);
		assertTrue(captured.getDevicesAttachedEvent().getHeader().getNodeUrnsList().contains(NODE_URN_1.toString()));
		assertTrue(captured.getDevicesAttachedEvent().getHeader().getNodeUrnsList().contains(NODE_URN_2.toString()));

		// this should be it
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	public void testFailedSendsAreEnqueuedAgain() throws Exception {

		handler.start();
		ChannelFuture future = mock(ChannelFuture.class);
		when(channel.write(any())).thenReturn(future);
		when(queue.dequeue()).thenReturn(empty());
		simulateConnect();

		// create a second future that is not used for the GatewayConnectedEvent but in this test
		ChannelFuture secondFuture = mock(ChannelFuture.class);
		when(channel.write(any())).thenReturn(secondFuture);

		// send a "random" upstream event on the event bus
		UpstreamMessageEvent event = createUpstreamMessageEvent();
		handler.onEvent(event);

		// verify that message is sent and capture message
		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(channel, times(2)).write(messageCaptor.capture());
		assertSame(event, messageCaptor.getValue().getUpstreamMessageEvent());

		// verify that listener is added to future
		ArgumentCaptor<ChannelFutureListener> futureListenerCaptor = ArgumentCaptor.forClass(ChannelFutureListener.class);
		verify(secondFuture).addListener(futureListenerCaptor.capture());

		// simulate failed sending
		when(secondFuture.isSuccess()).thenReturn(false);
		when(secondFuture.getCause()).thenReturn(mock(Throwable.class));
		futureListenerCaptor.getValue().operationComplete(secondFuture);

		// verify that message is enqueued for sending it later
		verify(queue).enqueue(same(messageCaptor.getValue()));
	}

	@Test
	public void testThatEventsAreEnqueuedAgainAfterDisconnection() throws Exception {
		ChannelFuture future = mock(ChannelFuture.class);
		when(channel.write(any())).thenReturn(future);
		when(future.isSuccess()).thenReturn(true);
		when(queue.dequeue()).thenReturn(empty());
		simulateConnect();
		simulateDisconnect();
		testQueuesEventsWhenNotConnectedInternal();
	}

	@Test
	public void testThatBroadcastsReceivedOverTheEventBusAreNotSentBack() throws Exception {
		ReservationStartedEvent event = MESSAGE_FACTORY.reservationStartedEvent(empty(), "abcdefgh", false);
		handler.onEvent(event);
		verifyNoMoreInteractions(channel);
	}

	@Test
	public void testThatIncomingDownstreamMessagesArePostedToTheEventBus() throws Exception {

		AreNodesAliveRequest request = MESSAGE_FACTORY.areNodesAliveRequest(empty(), empty(), newArrayList(NODE_URN_1, NODE_URN_2));
		Message msg = MessageWrapper.wrap(request);
		MessageEvent msgEvent = mock(MessageEvent.class);
		when(msgEvent.getMessage()).thenReturn(msg);

		handler.messageReceived(ctx, msgEvent);

		verify(eventBus).post(same(request));
	}

	@Test(expected = RuntimeException.class)
	public void testThatIncomingNonDownstreamMessagesResultInRuntimeException() throws Exception {

		DevicesAttachedEvent event = MESSAGE_FACTORY.devicesAttachedEvent(empty(), NODE_URN_1, NODE_URN_2);
		MessageHeaderPair pair = MessageHeaderPair.fromUnwrapped(event);
		MessageEvent msgEvent = mock(MessageEvent.class);
		when(msgEvent.getMessage()).thenReturn(pair);

		handler.messageReceived(ctx, msgEvent);
	}

	@Test
	public void testDevicesAttachedEventIsEnqueuedForDevicesConnectedEventWhenDisconnected() throws Exception {

		DevicesConnectedEvent event = new DevicesConnectedEvent(mock(DeviceAdapter.class), newHashSet(NODE_URN_1, NODE_URN_2));
		handler.onEvent(event);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(queue).enqueue(messageCaptor.capture());
		Message captured = messageCaptor.getValue();
		assertEquals(MessageType.EVENT_DEVICES_ATTACHED, captured.getType());
		assertTrue(captured.getDevicesAttachedEvent().getHeader().getNodeUrnsList().size() == 2);
		assertTrue(captured.getDevicesAttachedEvent().getHeader().getNodeUrnsList().contains(NODE_URN_1.toString()));
		assertTrue(captured.getDevicesAttachedEvent().getHeader().getNodeUrnsList().contains(NODE_URN_2.toString()));
	}

	@Test
	public void testDevicesDetachedEventIsSentForDevicesDisconnectedEvent() throws Exception {

		DevicesDisconnectedEvent event = new DevicesDisconnectedEvent(mock(DeviceAdapter.class), newHashSet(NODE_URN_1, NODE_URN_2));
		handler.onEvent(event);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(queue).enqueue(messageCaptor.capture());
		Message captured = messageCaptor.getValue();
		assertEquals(MessageType.EVENT_DEVICES_DETACHED, captured.getType());
		assertTrue(captured.getDevicesDetachedEvent().getHeader().getNodeUrnsList().size() == 2);
		assertTrue(captured.getDevicesDetachedEvent().getHeader().getNodeUrnsList().contains(NODE_URN_1.toString()));
		assertTrue(captured.getDevicesDetachedEvent().getHeader().getNodeUrnsList().contains(NODE_URN_2.toString()));
	}

	private void simulateConnect() throws Exception {
		handler.channelConnected(ctx, mock(ChannelStateEvent.class));
	}

	private void simulateDisconnect() throws Exception {
		handler.channelDisconnected(ctx, mock(ChannelStateEvent.class));
	}
}
