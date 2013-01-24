package de.uniluebeck.itm.tr.iwsn.pipeline;

import org.jboss.netty.channel.*;

public class EmbeddedChannelSink implements ChannelSink {

	public void eventSunk(ChannelPipeline pipeline, ChannelEvent e) {

		throw new RuntimeException("This should not have happened: " + e);
	}

	public void exceptionCaught(ChannelPipeline pipeline, ChannelEvent e, ChannelPipelineException cause)
			throws Exception {

		throw new RuntimeException("This should not have happened: ", cause);
	}

	public ChannelFuture execute(ChannelPipeline pipeline, Runnable task) {
		try {
			task.run();
			return Channels.succeededFuture(pipeline.getChannel());
		} catch (Throwable t) {
			return Channels.failedFuture(pipeline.getChannel(), t);
		}
	}
}