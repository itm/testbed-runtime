package de.uniluebeck.itm.tr.iwsn.pipeline;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.nettyprotocols.util.ChannelBufferTools.toPrintableString;

public class AbovePipelineLogger extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(AbovePipelineLogger.class);

	private final String nodeUrn;

	public AbovePipelineLogger(final String nodeUrn) {
		this.nodeUrn = nodeUrn;
	}

	@Override
	public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e)
			throws Exception {

		if (log.isTraceEnabled()) {

			log.trace("{} => Downstream to device above Pipeline: {}",
					nodeUrn,
					toPrintableString((ChannelBuffer) e.getMessage(), 200)
			);
		}
		super.writeRequested(ctx, e);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
			throws Exception {

		if (log.isTraceEnabled()) {

			log.trace("{} => Upstream from device above Pipeline: {}",
					nodeUrn,
					toPrintableString((ChannelBuffer) e.getMessage(), 200)
			);
		}
		super.messageReceived(ctx, e);
	}

}
