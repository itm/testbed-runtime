package de.uniluebeck.itm.tr.iwsn.common.netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionChannelHandler extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(ExceptionChannelHandler.class);

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		log.error("Uncaught exception: ", e.getCause());
		super.exceptionCaught(ctx, e);
	}
}