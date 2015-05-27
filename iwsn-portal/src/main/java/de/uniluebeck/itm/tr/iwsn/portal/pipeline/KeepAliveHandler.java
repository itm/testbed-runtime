package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageType;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;

/**
 * The KeepAliveHandler handles incoming idle state events (see {@link org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler}
 * by sending a {@link de.uniluebeck.itm.tr.iwsn.messages.Message} of type {@link de.uniluebeck.itm.tr.iwsn.messages.MessageType#KEEP_ALIVE}.
 * This way, the TCP channel is used for data and is not closed.
 * <p/>
 * If nothing has been received from the peer endpoint the KeepAliveHandler assumes that the peer died an unnatural death
 * and therefore closes the channel, indirectly informing other handlers of the termination of this connection.
 */
public class KeepAliveHandler extends IdleStateAwareChannelHandler {

	@Override
	public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {

		if (e.getState() == IdleState.READER_IDLE) {

			e.getChannel().close();

		} else if (e.getState() == IdleState.WRITER_IDLE) {

			Message keepAlive = Message
					.newBuilder()
					.setType(MessageType.KEEP_ALIVE_ACK)
					.build();

			e.getChannel().write(keepAlive);
		}
	}
}
