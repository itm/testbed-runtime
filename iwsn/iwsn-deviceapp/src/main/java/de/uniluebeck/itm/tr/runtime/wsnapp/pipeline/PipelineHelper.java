package de.uniluebeck.itm.tr.runtime.wsnapp.pipeline;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
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
	public static WSNAppMessages.SetChannelPipelineRequest.Builder convertToProtobuf(
			@Nonnull final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		WSNAppMessages.SetChannelPipelineRequest.Builder argumentBuilder =
				WSNAppMessages.SetChannelPipelineRequest.newBuilder();

		for (ChannelHandlerConfiguration channelHandlerConfiguration : channelHandlerConfigurations) {
			argumentBuilder.addChannelHandlerConfigurations(convertToProtobuf(channelHandlerConfiguration));
		}
		return argumentBuilder;
	}

	@Nonnull
	public static WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.Builder convertToProtobuf(
			@Nonnull final ChannelHandlerConfiguration channelHandlerConfiguration) {
		final WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.Builder configurationBuilder =
				WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration
						.newBuilder()
						.setName(channelHandlerConfiguration.getName());

		for (KeyValuePair keyValuePair : channelHandlerConfiguration.getConfiguration()) {
			configurationBuilder.addConfiguration(convertToProtobuf(keyValuePair));
		}
		return configurationBuilder;
	}

	@Nonnull
	public static WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.KeyValuePair.Builder convertToProtobuf(
			@Nonnull final KeyValuePair keyValuePair) {

		return WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.KeyValuePair
				.newBuilder()
				.setKey(keyValuePair.getKey())
				.setValue(keyValuePair.getValue());
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