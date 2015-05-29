package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageType;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import java.util.function.Function;

public class MessageUnwrapper extends OneToOneDecoder {

	public static final Function<Message, Object> MESSAGE_UNWRAP_FUNCTION = msg -> {

		if (msg.getType() == MessageType.KEEP_ALIVE || msg.getType() == MessageType.KEEP_ALIVE_ACK){
			throw new IllegalArgumentException("Keep alive messages contain nothing to unwrap");
		}

		if (EventMessageEvent.isWrappedEventMessageEvent(msg)) {
			return EventMessageEvent.fromWrapped(msg);
		} else if (RequestResponseMessageEvent.isWrappedRequestResponseMessage(msg)) {
			return RequestResponseMessageEvent.fromWrapped(msg);
		}

		throw new IllegalArgumentException("Unknown message type \"" + msg.getType() + "\"!");
	};

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object obj) throws Exception {

		if (!(obj instanceof Message)) {
			throw new IllegalArgumentException("Unknown message received! This probably means that the pipeline is " +
					"not configured correctly.");
		}

		final Message msg = (Message) obj;

		if (msg.getType() == MessageType.KEEP_ALIVE || msg.getType() == MessageType.KEEP_ALIVE_ACK) {
			// handled by de.uniluebeck.itm.tr.iwsn.portal.pipeline.KeepAliveHandler
			throw new IllegalArgumentException("Keep alive message should have been handled before!");
		}

		return MESSAGE_UNWRAP_FUNCTION.apply(msg);
	}
}
