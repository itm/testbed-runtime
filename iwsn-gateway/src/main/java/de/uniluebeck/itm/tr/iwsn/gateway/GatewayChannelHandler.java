package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Iterables.transform;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;

public class GatewayChannelHandler extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(GatewayChannelHandler.class);

	private final GatewayEventBus gatewayEventBus;

	private final EventIdProvider eventIdProvider;

	private final DeviceManager deviceManager;

	private Channel channel;

	@Inject
	public GatewayChannelHandler(final GatewayEventBus gatewayEventBus,
								 final EventIdProvider eventIdProvider,
								 final DeviceManager deviceManager) {

		this.gatewayEventBus = gatewayEventBus;
		this.eventIdProvider = eventIdProvider;
		this.deviceManager = deviceManager;
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		if (!(e.getMessage() instanceof Message)) {
			super.messageReceived(ctx, e);
		}

		final Message message = (Message) e.getMessage();
		switch (message.getType()) {
			case REQUEST:
				gatewayEventBus.post(((Message) e.getMessage()).getRequest());
				break;
			case EVENT_ACK:
				gatewayEventBus.post(((Message) e.getMessage()).getEventAck());
				break;
			default:
				throw new RuntimeException("Unexpected message type: " + message.getType());
		}
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

		log.trace("GatewayChannelHandler.channelConnected(ctx={}, event={})", ctx, e);

		channel = e.getChannel();
		gatewayEventBus.register(this);

		final Set<NodeUrn> connectedNodeUrns = deviceManager.getConnectedNodeUrns();

		if (!connectedNodeUrns.isEmpty()) {
			sendToPortal(newMessage(newEvent(eventIdProvider.get(), newDevicesAttachedEvent(connectedNodeUrns))));
		}

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("GatewayChannelHandler.channelDisconnected(ctx={}, event={}", ctx, e);
		gatewayEventBus.unregister(this);
		channel = null;
		super.channelDisconnected(ctx, e);
	}

	@Subscribe
	public void onUpstreamMessageEvent(UpstreamMessageEvent upstreamMessageEvent) {
		sendToPortal(newMessage(newEvent(eventIdProvider.get(), upstreamMessageEvent)));
	}

	@Subscribe
	public void onNotificationEvent(NotificationEvent notificationEvent) {
		sendToPortal(newMessage(newEvent(eventIdProvider.get(), notificationEvent)));
	}

	@Subscribe
	public void onGatewayDevicesAttachedEvent(DevicesAttachedEvent event) {

		final de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent dae =
				de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent
						.newBuilder()
						.addAllNodeUrns(transform(event.getNodeUrns(), toStringFunction()))
						.setTimestamp(now())
						.build();

		sendToPortal(newMessage(newEvent(eventIdProvider.get(), dae)));
	}

	@Subscribe
	public void onGatewayDevicesDetachedEvent(DevicesDetachedEvent event) {

		final de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent dde =
				de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent
						.newBuilder()
						.addAllNodeUrns(transform(event.getNodeUrns(), toStringFunction()))
						.setTimestamp(now())
						.build();

		sendToPortal(newMessage(newEvent(eventIdProvider.get(), dde)));
	}

	@Subscribe
	public void onDevicesAttachedEvent(de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent devicesAttachedEvent) {
		sendToPortal(newMessage(newEvent(eventIdProvider.get(), devicesAttachedEvent)));
	}

	@Subscribe
	public void onDevicesDetachedEvent(de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent devicesDetachedEvent) {
		sendToPortal(newMessage(newEvent(eventIdProvider.get(), devicesDetachedEvent)));
	}

	@Subscribe
	public void onEvent(Event event) {
		sendToPortal(newMessage(event));
	}

	@Subscribe
	public void onResponse(SingleNodeResponse singleNodeResponse) {
		sendToPortal(newMessage(singleNodeResponse));
	}

	@Subscribe
	public void onProgress(SingleNodeProgress singleNodeProgress) {
		sendToPortal(newMessage(singleNodeProgress));
	}

	@Override
	public String toString() {
		return "GatewayChannelHandler";
	}

	private void sendToPortal(final Message message) {
		log.trace("GatewayChannelHandler.sendToPortal(\nmessage={})", message);
		Channels.write(channel, message);
	}

	private long now() {
		return new DateTime().getMillis();
	}
}
