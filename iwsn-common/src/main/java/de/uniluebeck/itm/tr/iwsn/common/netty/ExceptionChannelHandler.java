package de.uniluebeck.itm.tr.iwsn.common.netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class ExceptionChannelHandler extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(ExceptionChannelHandler.class);

	private final Set<Class<? extends Throwable>> exceptionsIgnored = newHashSet();

	public ExceptionChannelHandler(Class<? extends Throwable>... exceptionsIgnored) {
		Collections.addAll(this.exceptionsIgnored, exceptionsIgnored);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		Throwable cause = e.getCause();
		assert cause != null;
		if (!exceptionsIgnored.contains(cause.getClass())) {
            log.error("Uncaught exception for remote address {}: ", ctx.getChannel().getRemoteAddress().toString(), cause);
            super.exceptionCaught(ctx, e);
        }
    }
}