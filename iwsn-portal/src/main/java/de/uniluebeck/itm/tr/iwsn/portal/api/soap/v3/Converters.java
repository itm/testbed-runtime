package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.v3.wsn.ChannelPipelinesMap;
import eu.wisebed.api.v3.wsn.Link;
import eu.wisebed.api.v3.wsn.VirtualLink;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;

public abstract class Converters {

	public static Map<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> convertToProto(
			List<ChannelPipelinesMap> maps) {
		final Map<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> resultMap = newHashMap();
		for (ChannelPipelinesMap map : maps) {
			for (NodeUrn nodeUrn : map.getNodeUrns()) {
				final GetChannelPipelinesResponse.GetChannelPipelineResponse.Builder responseBuilder =
						GetChannelPipelinesResponse.GetChannelPipelineResponse.newBuilder()
								.setNodeUrn(nodeUrn.toString())
								.addAllHandlerConfigurations(convertCHCToProto(map.getChannelHandlers()));
				resultMap.put(nodeUrn, responseBuilder.build());
			}
		}
		return resultMap;
	}

	public static List<de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration> convertCHCToProto(
			List<ChannelHandlerConfiguration> configs) {
		final List<de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration> result = newArrayList();
		for (ChannelHandlerConfiguration config : configs) {
			final List<de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair> keyValuePairs =
					convertKVPToProto(config.getConfiguration());
			result.add(de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration
					.newBuilder()
					.setName(config.getName())
					.addAllConfiguration(keyValuePairs)
					.build()
			);
		}
		return result;
	}

	public static List<de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair> convertKVPToProto(
			final List<KeyValuePair> configuration) {
		final List<de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair> targetList =
				newArrayList();
		for (KeyValuePair keyValuePair : configuration) {
			targetList.add(de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair.newBuilder()
					.setKey(keyValuePair.getKey())
					.setValue(keyValuePair.getValue())
					.build()
			);
		}
		return targetList;
	}

	public static List<ChannelPipelinesMap> convert(
			final Map<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> resultMap) {

		final List<ChannelPipelinesMap> resultList = newArrayList();

		for (Map.Entry<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> entry : resultMap.entrySet()) {

			ChannelPipelinesMap map = new ChannelPipelinesMap();
			map.getNodeUrns().add(entry.getKey());

			final GetChannelPipelinesResponse.GetChannelPipelineResponse perNodeResponse = entry.getValue();

			List<de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration> handlerConfigs =
					perNodeResponse.getHandlerConfigurationsList();

			for (de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration handlerConfig : handlerConfigs) {
				final ChannelHandlerConfiguration chc = new ChannelHandlerConfiguration();
				chc.setName(handlerConfig.getName());
				for (de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair keyValuePair : handlerConfig
						.getConfigurationList()) {
					chc.getConfiguration().add(convert(keyValuePair));
				}
				map.getChannelHandlers().add(chc);
			}

			resultList.add(map);
		}
		return resultList;
	}

	public static KeyValuePair convert(
			final de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair source) {
		final KeyValuePair target = new KeyValuePair();
		target.setKey(source.getKey());
		target.setValue(source.getValue());
		return target;
	}

	public static Multimap<NodeUrn, NodeUrn> convertLinksToMap(final List<Link> links) {
		final Multimap<NodeUrn, NodeUrn> map = HashMultimap.create();
		for (Link link : links) {
			map.put(link.getSourceNodeUrn(), link.getTargetNodeUrn());
		}
		return map;
	}

	public static Multimap<NodeUrn, NodeUrn> convertVirtualLinks(final List<VirtualLink> links) {
		final Multimap<NodeUrn, NodeUrn> map = HashMultimap.create();
		for (VirtualLink link : links) {
			map.put(link.getSourceNodeUrn(), link.getTargetNodeUrn());
		}
		return map;
	}

	public static Iterable<? extends de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration> convertCHCs(
			final List<ChannelHandlerConfiguration> chcs) {

		List<de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration> retList = newLinkedList();

		for (ChannelHandlerConfiguration chc : chcs) {

			final de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.Builder builder =
					de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.newBuilder()
							.setName(chc.getName());

			for (KeyValuePair keyValuePair : chc.getConfiguration()) {
				builder.addConfigurationBuilder()
						.setKey(keyValuePair.getKey())
						.setValue(keyValuePair.getValue());
			}

			retList.add(builder.build());
		}

		return retList;
	}

	public static List<ChannelHandlerConfiguration> convertToSOAP(
			final List<de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration> chcs) {
		final List<ChannelHandlerConfiguration> targetList = newArrayList();
		for (de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration chc : chcs) {
			targetList.add(convertToSOAP(chc));
		}
		return targetList;
	}

	public static ChannelHandlerConfiguration convertToSOAP(
			final de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration chc) {
		final ChannelHandlerConfiguration target = new ChannelHandlerConfiguration();
		target.setName(chc.getName());
		for (de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair kvp : chc
				.getConfigurationList()) {
			target.getConfiguration().add(convertToSOAP(kvp));
		}
		return target;
	}

	public static KeyValuePair convertToSOAP(
			final de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair kvp) {
		return new KeyValuePair().withKey(kvp.getKey()).withValue(kvp.getValue());
	}
}
