package de.uniluebeck.itm.tr.runtime.wsnapp.pipeline;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineException;
import org.jboss.netty.channel.ChannelSink;

public class EmbeddedChannelSink implements ChannelSink {

	public void eventSunk(ChannelPipeline pipeline, ChannelEvent e) {

		throw new RuntimeException("This should not have happened.");
	}

	public void exceptionCaught(ChannelPipeline pipeline, ChannelEvent e, ChannelPipelineException cause)
			throws Exception {

		throw new RuntimeException("This should not have happened.");
	}
}