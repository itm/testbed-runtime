package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.EventAck;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import org.jboss.netty.channel.*;

public class PortalChannelHandler extends SimpleChannelHandler {

	private final PortalEventBus portalEventBus;

	private Channel channel;

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
				Event event = message.getEvent();
				portalEventBus.post(event);
				Channels.write(channel, EventAck.newBuilder().setEventId(event.getEventId()));
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

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		channel = e.getChannel();
		portalEventBus.register(this);
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		portalEventBus.unregister(this);
		channel = null;
		super.channelDisconnected(ctx, e);
	}

	@Subscribe
	public void onEventAck(EventAck eventAck) {
		Channels.write(channel, eventAck);
	}

	@Subscribe
	public void onRequest(Request request) {
		Channels.write(channel, request);
	}
}
