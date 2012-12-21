package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeProgress;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import org.jboss.netty.channel.*;

public class GatewayChannelHandler extends SimpleChannelHandler {

	private final GatewayEventBus gatewayEventBus;

	private Channel channel;

	@Inject
	public GatewayChannelHandler(final GatewayEventBus gatewayEventBus) {
		this.gatewayEventBus = gatewayEventBus;
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
		channel = e.getChannel();
		gatewayEventBus.register(this);
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		gatewayEventBus.unregister(this);
		channel = null;
		super.channelDisconnected(ctx, e);
	}

	@Subscribe
	public void onEvent(Event event) {
		Channels.write(channel, event);
	}

	@Subscribe
	public void onResponse(SingleNodeResponse singleNodeResponse) {
		Channels.write(channel, singleNodeResponse);
	}

	@Subscribe
	public void onProgress(SingleNodeProgress singleNodeProgress) {
		Channels.write(channel, singleNodeProgress);
	}
}
