package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.common.base.Function;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.STRING_TO_NODE_URN;

public abstract class RequestHelper {

	public static final Function<Link, NodeUrn> LINK_SOURCE_TO_NODE_URN = new Function<Link, NodeUrn>() {
		@Override
		public NodeUrn apply(final Link input) {
			return new NodeUrn(input.getSourceNodeUrn());
		}
	};

	public static Set<NodeUrn> extractNodeUrns(final Request request) {
		switch (request.getType()) {

			case ARE_NODES_ALIVE:
				return newHashSet(transform(request.getAreNodesAliveRequest().getNodeUrnsList(), STRING_TO_NODE_URN));

			case ARE_NODES_CONNECTED:
				return newHashSet(
						transform(request.getAreNodesConnectedRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
				);

			case DISABLE_NODES:
				return newHashSet(transform(request.getDisableNodesRequest().getNodeUrnsList(), STRING_TO_NODE_URN));

			case DISABLE_VIRTUAL_LINKS:
				return newHashSet(
						transform(request.getDisableVirtualLinksRequest().getLinksList(), LINK_SOURCE_TO_NODE_URN)
				);

			case DISABLE_PHYSICAL_LINKS:
				return newHashSet(
						transform(request.getDisablePhysicalLinksRequest().getLinksList(), LINK_SOURCE_TO_NODE_URN)
				);

			case ENABLE_NODES:
				return newHashSet(transform(request.getEnableNodesRequest().getNodeUrnsList(), STRING_TO_NODE_URN));

			case ENABLE_PHYSICAL_LINKS:
				return newHashSet(
						transform(request.getEnablePhysicalLinksRequest().getLinksList(), LINK_SOURCE_TO_NODE_URN)
				);

			case ENABLE_VIRTUAL_LINKS:
				return newHashSet(
						transform(request.getEnableVirtualLinksRequest().getLinksList(), LINK_SOURCE_TO_NODE_URN)
				);

			case FLASH_IMAGES:
				return newHashSet(transform(request.getFlashImagesRequest().getNodeUrnsList(), STRING_TO_NODE_URN));

			case RESET_NODES:
				return newHashSet(transform(request.getResetNodesRequest().getNodeUrnsList(), STRING_TO_NODE_URN));

			case SEND_DOWNSTREAM_MESSAGES:
				return newHashSet(transform(
						request.getSendDownstreamMessagesRequest().getTargetNodeUrnsList(),
						STRING_TO_NODE_URN
				)
				);

			case SET_CHANNEL_PIPELINES:
				return newHashSet(transform(
						request.getSetChannelPipelinesRequest().getNodeUrnsList(),
						STRING_TO_NODE_URN
				)
				);

			default:
				throw new RuntimeException("Unknown request type \"" + request.getType() + "\"!");
		}
	}
}
