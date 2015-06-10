package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.gateway.eventqueue.UpstreamMessageQueue;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
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

	private final UpstreamMessageQueue upstreamMessageQueue;

	private final MessageFactory messageFactory;

	private Channel channel;

	@Inject
	public GatewayChannelHandler(final GatewayEventBus gatewayEventBus,
								 final DeviceManager deviceManager,
								 final UpstreamMessageQueue upstreamMessageQueue,
								 final MessageFactory messageFactory) {
		this.gatewayEventBus = gatewayEventBus;
		this.deviceManager = deviceManager;
		this.upstreamMessageQueue = upstreamMessageQueue;
		this.messageFactory = messageFactory;
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		if (!(e.getMessage() instanceof MessageHeaderPair)) {
			throw new IllegalArgumentException("Expected " + MessageHeaderPair.class.getCanonicalName() +
					", got " + e.getMessage().getClass().getCanonicalName() +
					". Pipeline seems to be misconfigured!");
		}

		final MessageHeaderPair pair = (MessageHeaderPair) e.getMessage();

		if (!pair.header.getDownstream()) {
			throw new RuntimeException("Unexpected non-downstream message type: " + pair.header.getType());
		}

		log.trace("GatewayChannelHandler.messageReceived(): posting downstream message on GatewayEventBus: {}", pair.message);
		gatewayEventBus.post(pair.message);
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

		log.trace("GatewayChannelHandler.channelConnected(ctx={}, event={})", ctx, e);

		if (!upstreamMessageQueue.isRunning()) {
			upstreamMessageQueue.awaitRunning(5, TimeUnit.SECONDS);
			if (!upstreamMessageQueue.isRunning()) {
				throw new RuntimeException("GatewayChannelHandler.channelConnected(): UpstreamMessageQueue should be running. Timed out waiting for it!");
			}
		}

		channel = e.getChannel();
		upstreamMessageQueue.channelConnected(channel);

		final String hostname = InetAddress.getLocalHost().getHostAddress();
		upstreamMessageQueue.enqueue(MessageWrapper.wrap(messageFactory.gatewayConnectedEvent(Optional.empty(), hostname)));

		final Set<NodeUrn> connectedNodeUrns = deviceManager.getConnectedNodeUrns();

		if (!connectedNodeUrns.isEmpty()) {
			upstreamMessageQueue.enqueue(MessageWrapper.wrap(messageFactory.devicesAttachedEvent(Optional.empty(), connectedNodeUrns)));
		}

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("GatewayChannelHandler.channelDisconnected(ctx={}, event={}", ctx, e);
		upstreamMessageQueue.channelDisconnected();
		channel = null;
		super.channelDisconnected(ctx, e);
	}

	@Override
	public String toString() {
		return "GatewayChannelHandler";
	}
}
