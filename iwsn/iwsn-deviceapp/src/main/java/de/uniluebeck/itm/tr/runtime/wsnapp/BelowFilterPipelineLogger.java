package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.netty.handlerstack.util.ChannelBufferTools;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BelowFilterPipelineLogger extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(BelowFilterPipelineLogger.class);

	private final String nodeUrn;

	public BelowFilterPipelineLogger(final String nodeUrn) {
		this.nodeUrn = nodeUrn;
	}

	@Override
	public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e)
			throws Exception {
		log.debug("{} => Downstream to device below FilterPipeline: {}",
				nodeUrn,
				ChannelBufferTools.toPrintableString((ChannelBuffer) e.getMessage(), 200)
		);
		super.writeRequested(ctx, e);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
			throws Exception {
		log.debug("{} => Upstream from device below FilterPipeline: {}",
				nodeUrn,
				ChannelBufferTools.toPrintableString((ChannelBuffer) e.getMessage(), 200)
		);
		super.messageReceived(ctx, e);
	}
}
