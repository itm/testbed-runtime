package de.uniluebeck.itm.tr.iwsn.pipeline;

import de.uniluebeck.itm.nettyprotocols.NamedChannelHandler;
import de.uniluebeck.itm.nettyprotocols.NamedChannelHandlerList;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class PipelineHelper {

	@Nonnull
	public static ChannelPipeline setPipeline(@Nonnull final ChannelPipeline pipeline,
											  @Nonnull final NamedChannelHandlerList handlers) {

		checkNotNull(pipeline);
		checkNotNull(handlers);

		clearPipeline(pipeline);

		for (NamedChannelHandler handler : handlers) {
			pipeline.addLast(handler.getInstanceName(), handler.getChannelHandler());
		}

		return pipeline;
	}

	private static void clearPipeline(final ChannelPipeline pipeline) {
		ChannelHandler handler;
		while ((handler = pipeline.getFirst()) != null) {
			pipeline.remove(handler);
		}
	}

}