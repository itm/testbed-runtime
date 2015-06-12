package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.gateway.eventqueue.UpstreamMessageQueue;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GatewayChannelHandler extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(GatewayChannelHandler.class);

	private final GatewayEventBus gatewayEventBus;

	private final DeviceManager deviceManager;

	private final UpstreamMessageQueue queue;

	private final MessageFactory messageFactory;

	private final Object channelLock = new Object();

	private Channel channel;

	@Inject
	public GatewayChannelHandler(final GatewayEventBus gatewayEventBus,
								 final DeviceManager deviceManager,
								 final UpstreamMessageQueue queue,
								 final MessageFactory messageFactory) {
		this.gatewayEventBus = gatewayEventBus;
		this.deviceManager = deviceManager;
		this.queue = queue;
		this.messageFactory = messageFactory;
	}

	public void start() {
		queue.startAsync().awaitRunning();
		gatewayEventBus.register(this);
	}

	public void stop() {
		gatewayEventBus.unregister(this);
		queue.stopAsync().awaitTerminated();
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		if (!(e.getMessage() instanceof Message)) {
			throw new IllegalArgumentException("Expected " + Message.class.getCanonicalName() +
					", got " + e.getMessage().getClass().getCanonicalName() +
					". Pipeline seems to be misconfigured!");
		}

		final MessageHeaderPair pair = MessageHeaderPair.fromWrapped(e.getMessage());

		if (!pair.header.getDownstream()) {
			throw new RuntimeException("Unexpected non-downstream message type: " + pair.header.getType());
		}

		log.trace("GatewayChannelHandler.messageReceived(): posting downstream message on GatewayEventBus: {}", pair.message);
		gatewayEventBus.post(pair.message);
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

		log.trace("GatewayChannelHandler.channelConnected(ctx={}, event={})", ctx, e);

		if (!queue.isRunning()) {
			queue.awaitRunning(5, TimeUnit.SECONDS);
			if (!queue.isRunning()) {
				throw new RuntimeException("GatewayChannelHandler.channelConnected(): UpstreamMessageQueue should be running. Timed out waiting for it!");
			}
		}

		synchronized (channelLock) {
			channel = ctx.getChannel();
		}

		final String hostname = InetAddress.getLocalHost().getHostAddress();
		sendMessage(MessageWrapper.wrap(messageFactory.gatewayConnectedEvent(Optional.empty(), hostname)));

		Optional<Message> dequeued;

		while ((dequeued = queue.dequeue()).isPresent()) {
			sendMessage(dequeued.get());
		}

		final Set<NodeUrn> connectedNodeUrns = deviceManager.getConnectedNodeUrns();

		if (!connectedNodeUrns.isEmpty()) {
			sendMessage(MessageWrapper.wrap(messageFactory.devicesAttachedEvent(Optional.empty(), connectedNodeUrns)));
		}

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("GatewayChannelHandler.channelDisconnected(ctx={}, event={}", ctx, e);
		synchronized (channelLock) {
			channel = null;
		}
		super.channelDisconnected(ctx, e);
	}

	@Override
	public String toString() {
		return "GatewayChannelHandler";
	}

	private void sendMessage(Message message) {

		if (!isConnected()) {
			queue.enqueue(message);
			return;
		}

		ChannelFuture channelFuture;
		synchronized (channelLock) {
			if (!isConnected()) {
				queue.enqueue(message);
			}

			channelFuture = channel.write(message);
		}
		channelFuture.addListener(cf -> {
			if (!cf.isSuccess()) {
				log.warn(
						"Sending message {} to portal not successful, enqueuing message to try again later. Cause: {}",
						message,
						cf.getCause()
				);
				queue.enqueue(message);
			}
		});
	}

	private boolean isConnected() {
		synchronized (channelLock) {
			return channel != null;
		}
	}

	@Subscribe
	public void onEvent(MessageLite obj) {

		if (MessageHeaderPair.isUnwrappedMessageEvent(obj)) {

			final MessageHeaderPair pair = MessageHeaderPair.fromUnwrapped(obj);

			// only messages towards portal (upstream) should be sent
			// broadcasts are messages that have been sent as broadcast to all gateways by the portal, don't send back
			if (pair.header.getUpstream()) {

				log.trace("UpstreamMessageQueueImpl.onEvent(): enqueuing upstream message {}", obj);
				sendMessage(MessageWrapper.WRAP_FUNCTION.apply(pair.message));
			}
		}
	}

	@Subscribe
	public void onEvent(DevicesConnectedEvent event) {
		log.info("UpstreamMessageQueueImpl.onEvent(): enqueuing DevicesAttachedEvent for {}", event.getNodeUrns());
		final DevicesAttachedEvent dae = messageFactory.devicesAttachedEvent(Optional.empty(), event.getNodeUrns());
		sendMessage(MessageWrapper.wrap(dae));
	}

	@Subscribe
	public void onEvent(DevicesDisconnectedEvent event) {
		log.info("UpstreamMessageQueueImpl.onEvent(): enqueuing DevicesDetachedEvent for {}", event.getNodeUrns());
		DevicesDetachedEvent dde = messageFactory.devicesDetachedEvent(Optional.empty(), event.getNodeUrns());
		sendMessage(MessageWrapper.wrap(dde));
	}
}
