package de.uniluebeck.itm.tr.iwsn.messages;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

public abstract class MessageUtils {

	public static Set<NodeUrn> getNodeUrns(final AreNodesAliveRequest request) {
		return toNodeUrnSet(request.getNodeUrnsList());
	}

	public static Set<NodeUrn> getNodeUrns(final AreNodesConnectedRequest request) {
		return toNodeUrnSet(request.getNodeUrnsList());
	}

	public static Set<NodeUrn> getNodeUrns(final Bla request) {
		switch (request.getType()) {

			case DISABLE_NODES:
				return toNodeUrnSet(request.getDisableNodesRequest().getNodeUrnsList());

			case DISABLE_PHYSICAL_LINKS:
				return getSourceNodeUrnsFromLinks(request.getDisablePhysicalLinksRequest().getLinksList());

			case DISABLE_VIRTUAL_LINKS:
				return getSourceNodeUrnsFromLinks(request.getDisableVirtualLinksRequest().getLinksList());

			case ENABLE_NODES:
				return toNodeUrnSet(request.getEnableNodesRequest().getNodeUrnsList());

			case ENABLE_PHYSICAL_LINKS:
				return getSourceNodeUrnsFromLinks(request.getEnablePhysicalLinksRequest().getLinksList());

			case ENABLE_VIRTUAL_LINKS:
				return getSourceNodeUrnsFromLinks(request.getEnableVirtualLinksRequest().getLinksList());

			case FLASH_IMAGES:
				return toNodeUrnSet(request.getFlashImagesRequest().getNodeUrnsList());

			case GET_CHANNEL_PIPELINES:
				return toNodeUrnSet(request.getGetChannelPipelinesRequest().getNodeUrnsList());

			case RESET_NODES:
				return toNodeUrnSet(request.getResetNodesRequest().getNodeUrnsList());

			case SEND_DOWNSTREAM_MESSAGES:
				return toNodeUrnSet(request.getSendDownstreamMessagesRequest().getTargetNodeUrnsList());

			case SET_CHANNEL_PIPELINES:
				return toNodeUrnSet(request.getSetChannelPipelinesRequest().getNodeUrnsList());

			default:
				throw new RuntimeException("Unknown request type received!");
		}
	}

	public static boolean isErrorStatusCode(final Request request, final SingleNodeResponse response) {
		return response.getStatusCode() == getUnconnectedStatusCode(request) || response.getStatusCode() < 0;
	}

	public static int getUnconnectedStatusCode(final Request request) {
		switch (request.getType()) {
			case ARE_NODES_ALIVE:
				return 0;
			case ARE_NODES_CONNECTED:
				return 0;
			default:
				return -1;
		}
	}

	private static HashSet<NodeUrn> toNodeUrnSet(final List<String> nodeUrnsList) {
		return newHashSet(
				transform(
						nodeUrnsList,
						STRING_TO_NODE_URN
				)
		);
	}

	private static Set<NodeUrn> getSourceNodeUrnsFromLinks(final Iterable<Link> links) {
		final Set<NodeUrn> nodeUrns = newHashSet();
		for (Link link : links) {
			nodeUrns.add(new NodeUrn(link.getSourceNodeUrn()));
		}
		return nodeUrns;
	}
}
