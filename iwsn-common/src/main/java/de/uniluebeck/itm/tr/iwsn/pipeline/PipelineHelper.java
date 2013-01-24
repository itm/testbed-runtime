package de.uniluebeck.itm.tr.iwsn.pipeline;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;

import javax.annotation.Nonnull;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PipelineHelper {

	@Nonnull
	public static List<Tuple<String, Multimap<String, String>>> convertCHCList(
			@Nonnull final List<ChannelHandlerConfiguration> in) {

		List<Tuple<String, Multimap<String, String>>> out = Lists.newArrayList();
		for (ChannelHandlerConfiguration configuration : in) {
			out.add(convertCHC(configuration));
		}
		return out;
	}

	@Nonnull
	public static Tuple<String, Multimap<String, String>> convertCHC(
			@Nonnull final ChannelHandlerConfiguration configuration) {
		return new Tuple<String, Multimap<String, String>>(
				configuration.getName(),
				convertKeyValuePair(configuration.getConfiguration())
		);
	}

	@Nonnull
	public static Multimap<String, String> convertKeyValuePair(@Nonnull final List<KeyValuePair> configuration) {
		Multimap<String, String> out = HashMultimap.create();
		for (KeyValuePair keyValuePair : configuration) {
			out.put(keyValuePair.getKey(), keyValuePair.getValue());
		}
		return out;
	}

	@Nonnull
	public static ChannelPipeline setPipeline(@Nonnull final ChannelPipeline pipeline,
											  @Nonnull final List<Tuple<String, ChannelHandler>> handlers) {

		checkNotNull(pipeline);
		checkNotNull(handlers);

		clearPipeline(pipeline);

		for (Tuple<String, ChannelHandler> handler : handlers) {
			pipeline.addLast(handler.getFirst(), handler.getSecond());
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