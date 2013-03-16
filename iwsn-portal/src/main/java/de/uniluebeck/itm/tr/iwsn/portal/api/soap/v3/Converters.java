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

public abstract class Converters {

	public static List<ChannelPipelinesMap> convert(
			final Map<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> resultMap) {
		final List<ChannelPipelinesMap> resultList = newArrayList();
		for (Map.Entry<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> entry : resultMap .entrySet()) {

			ChannelPipelinesMap map = new ChannelPipelinesMap();
			map.getNodeUrns().add(entry.getKey());

			de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration pipeline = entry.getValue().getPipeline();

			ChannelHandlerConfiguration chc = new ChannelHandlerConfiguration();
			chc.setName(pipeline.getName());
			for (de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair keyValuePair : pipeline
					.getConfigurationList()) {
				chc.getConfiguration().add(convert(keyValuePair));
			}

			map.getChannelHandlers().add(chc);
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

	public static  Multimap<NodeUrn, NodeUrn> convertLinksToMap(final List<Link> links) {
		final Multimap<NodeUrn, NodeUrn> map = HashMultimap.create();
		for (Link link : links) {
			map.put(link.getSourceNodeUrn(), link.getTargetNodeUrn());
		}
		return map;
	}

	public static  Multimap<NodeUrn, NodeUrn> convertVirtualLinks(final List<VirtualLink> links) {
		final Multimap<NodeUrn, NodeUrn> map = HashMultimap.create();
		for (VirtualLink link : links) {
			map.put(link.getSourceNodeUrn(), link.getTargetNodeUrn());
		}
		return map;
	}

	public static  Iterable<? extends de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration> convertCHCs(
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
}
