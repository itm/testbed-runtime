package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.EventAck;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.netty.channel.Channels.write;

public class PortalChannelHandler extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(PortalChannelHandler.class);

	private final PortalEventBus portalEventBus;

	private final BiMap<NodeUrn, ChannelHandlerContext> nodeToContextMap =
			Maps.synchronizedBiMap(HashBiMap.<NodeUrn, ChannelHandlerContext>create());

	private final ChannelGroup allChannels = new DefaultChannelGroup();

	@Inject
	public PortalChannelHandler(final PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		if (!(e.getMessage() instanceof Message)) {
			super.messageReceived(ctx, e);
		}

		final Message message = (Message) e.getMessage();
		switch (message.getType()) {
			case EVENT:
				final Event event = message.getEvent();
				portalEventBus.post(event);
				sendEventAck(ctx, event);
				processEvent(ctx, event);
				break;
			case PROGRESS:
				portalEventBus.post(message.getProgress());
				break;
			case RESPONSE:
				portalEventBus.post(message.getResponse());
				break;
			default:
				throw new RuntimeException("Unexpected message type: " + message.getType());
		}
	}

	private void processEvent(final ChannelHandlerContext ctx, final Event event) {
		switch (event.getType()) {
			case DEVICES_ATTACHED:
				for (String nodeUrnString : event.getDevicesAttachedEvent().getNodeUrnsList()) {
					nodeToContextMap.put(new NodeUrn(nodeUrnString), ctx);
				}
				break;
			case DEVICES_DETACHED:
				for (String nodeUrnString : event.getDevicesDetachedEvent().getNodeUrnsList()) {
					nodeToContextMap.remove(new NodeUrn(nodeUrnString));
				}
				break;
		}
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.debug("Channel connected: {}", e);
		if (allChannels.isEmpty()) {
			log.debug("Subscribing to the event bus.");
			portalEventBus.register(this);
		}
		allChannels.add(e.getChannel());
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.debug("Channel disconnected: {}", e);
		allChannels.remove(e.getChannel());
		if (allChannels.isEmpty()) {
			log.debug("Unsubscribing from the event bus.");
			portalEventBus.unregister(this);
		}
		super.channelDisconnected(ctx, e);
	}

	@Subscribe
	public void onRequest(final Request request) {
		// TODO implement
		throw new RuntimeException("TODO implement multicast request handling");
	}

	private void sendEventAck(final ChannelHandlerContext ctx, final Event event) {

		final EventAck.Builder eventAck = EventAck.newBuilder().setEventId(event.getEventId());
		final DefaultChannelFuture channelFuture = new DefaultChannelFuture(ctx.getChannel(), true);
		final Message message = Message.newBuilder().setType(Message.Type.EVENT_ACK).setEventAck(eventAck).build();

		write(ctx, channelFuture, message);
	}
}
