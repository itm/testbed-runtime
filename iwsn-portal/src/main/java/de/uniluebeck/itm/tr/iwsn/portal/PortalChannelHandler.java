package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static org.jboss.netty.channel.Channels.write;

public class PortalChannelHandler extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(PortalChannelHandler.class);

	private final PortalEventBus portalEventBus;

	private final Multimap<ChannelHandlerContext, NodeUrn> contextToNodeUrnsMap = HashMultimap.create();

	private final ChannelGroup allChannels = new DefaultChannelGroup();

	private static final Function<String, NodeUrn> STRING_TO_NODE_URN = new Function<String, NodeUrn>() {
		@Nullable
		@Override
		public NodeUrn apply(@Nullable final String input) {
			return new NodeUrn(input);
		}
	};

	@Inject
	public PortalChannelHandler(final PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Subscribe
	public void onRequest(final Request request) {

		final long requestId = request.getRequestId();
		final List<String> nodeUrnsList;
		final Set<NodeUrn> nodeUrns;
		final Multimap<ChannelHandlerContext, NodeUrn> mapping;
		final Map<ChannelHandlerContext, Request> requestsToBeSent = newHashMap();
		final List<Link> links;

		switch (request.getType()) {

			case ARE_NODES_ALIVE:

				nodeUrnsList = request.getAreNodesAliveRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					requestsToBeSent.put(ctx, newAreNodesAliveRequest(requestId, mapping.get(ctx)));
				}

				sendRequests(requestsToBeSent);
				break;

			case ARE_NODES_CONNECTED:

				nodeUrnsList = request.getAreNodesConnectedRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					requestsToBeSent.put(ctx, newAreNodesConnectedRequest(requestId, mapping.get(ctx)));
				}

				sendRequests(requestsToBeSent);
				break;

			case DISABLE_NODES:

				nodeUrnsList = request.getDisableNodesRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					requestsToBeSent.put(ctx, newDisableNodesRequest(requestId, mapping.get(ctx)));
				}

				sendRequests(requestsToBeSent);
				break;

			case DISABLE_PHYSICAL_LINKS:

				links = request.getDisablePhysicalLinksRequest().getLinksList();

				nodeUrnsList = newArrayList();
				for (Link link : links) {
					nodeUrnsList.add(link.getSourceNodeUrn());
				}
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					final Multimap<NodeUrn, NodeUrn> subRequestLinks = HashMultimap.create();
					for (Link link : links) {
						if (Iterables.contains(mapping.get(ctx), new NodeUrn(link.getSourceNodeUrn()))) {
							subRequestLinks.put(
									new NodeUrn(link.getSourceNodeUrn()),
									new NodeUrn(link.getTargetNodeUrn())
							);
						}
					}
					requestsToBeSent.put(ctx, newDisablePhysicalLinksRequest(requestId, subRequestLinks));
				}

				sendRequests(requestsToBeSent);
				break;

			case DISABLE_VIRTUAL_LINKS:

				links = request.getDisableVirtualLinksRequest().getLinksList();

				nodeUrnsList = newArrayList();
				for (Link link : links) {
					nodeUrnsList.add(link.getSourceNodeUrn());
				}
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					final Multimap<NodeUrn, NodeUrn> subRequestLinks = HashMultimap.create();
					for (Link link : links) {
						if (Iterables.contains(mapping.get(ctx), new NodeUrn(link.getSourceNodeUrn()))) {
							subRequestLinks.put(
									new NodeUrn(link.getSourceNodeUrn()),
									new NodeUrn(link.getTargetNodeUrn())
							);
						}
					}
					requestsToBeSent.put(ctx, newDisableVirtualLinksRequest(requestId, subRequestLinks));
				}

				sendRequests(requestsToBeSent);
				break;

			case ENABLE_NODES:

				nodeUrnsList = request.getEnableNodesRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					requestsToBeSent.put(ctx, newEnableNodesRequest(requestId, mapping.get(ctx)));
				}

				sendRequests(requestsToBeSent);
				break;

			case ENABLE_PHYSICAL_LINKS:

				links = request.getEnablePhysicalLinksRequest().getLinksList();

				nodeUrnsList = newArrayList();
				for (Link link : links) {
					nodeUrnsList.add(link.getSourceNodeUrn());
				}
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					final Multimap<NodeUrn, NodeUrn> subRequestLinks = HashMultimap.create();
					for (Link link : links) {
						if (Iterables.contains(mapping.get(ctx), new NodeUrn(link.getSourceNodeUrn()))) {
							subRequestLinks.put(
									new NodeUrn(link.getSourceNodeUrn()),
									new NodeUrn(link.getTargetNodeUrn())
							);
						}
					}
					requestsToBeSent.put(ctx, newEnablePhysicalLinksRequest(requestId, subRequestLinks));
				}

				sendRequests(requestsToBeSent);
				break;

			case ENABLE_VIRTUAL_LINKS:

				links = request.getEnableVirtualLinksRequest().getLinksList();

				nodeUrnsList = newArrayList();
				for (Link link : links) {
					nodeUrnsList.add(link.getSourceNodeUrn());
				}
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					final Multimap<NodeUrn, NodeUrn> subRequestLinks = HashMultimap.create();
					for (Link link : links) {
						if (Iterables.contains(mapping.get(ctx), new NodeUrn(link.getSourceNodeUrn()))) {
							subRequestLinks.put(
									new NodeUrn(link.getSourceNodeUrn()),
									new NodeUrn(link.getTargetNodeUrn())
							);
						}
					}
					requestsToBeSent.put(ctx, newEnableVirtualLinksRequest(requestId, subRequestLinks));
				}

				sendRequests(requestsToBeSent);
				break;

			case FLASH_IMAGES:

				nodeUrnsList = request.getFlashImagesRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					final ByteString imageBytes = request.getFlashImagesRequest().getImage();
					requestsToBeSent.put(ctx, newFlashImagesRequest(requestId, mapping.get(ctx), imageBytes));
				}

				sendRequests(requestsToBeSent);
				break;

			case RESET_NODES:

				nodeUrnsList = request.getResetNodesRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					requestsToBeSent.put(ctx, newResetNodesRequest(requestId, mapping.get(ctx)));
				}

				sendRequests(requestsToBeSent);
				break;

			case SEND_DOWNSTREAM_MESSAGES:

				nodeUrnsList = request.getSendDownstreamMessagesRequest().getTargetNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					final ByteString bytes = request.getSendDownstreamMessagesRequest().getMessageBytes();
					requestsToBeSent.put(ctx, newSendDownstreamMessageRequest(requestId, mapping.get(ctx), bytes));
				}

				sendRequests(requestsToBeSent);
				break;

			case SET_CHANNEL_PIPELINES:

				nodeUrnsList = request.getSetChannelPipelinesRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {
					final List<SetChannelPipelinesRequest.ChannelHandlerConfiguration> configs =
							request.getSetChannelPipelinesRequest().getChannelHandlerConfigurationsList();
					requestsToBeSent.put(ctx, newSetChannelPipelinesRequest(requestId, mapping.get(ctx), configs));
				}

				sendRequests(requestsToBeSent);
				break;

			default:
				throw new RuntimeException("Unknown request type received!");
		}
	}

	private void sendRequests(final Map<ChannelHandlerContext, Request> requestsToBeSent) {
		for (Map.Entry<ChannelHandlerContext, Request> entry : requestsToBeSent.entrySet()) {
			final ChannelHandlerContext ctx = entry.getKey();
			final Request requestBuilder = entry.getValue();
			final Message message = Message.newBuilder()
					.setType(Message.Type.REQUEST)
					.setRequest(requestBuilder)
					.build();
			Channels.write(ctx, new DefaultChannelFuture(ctx.getChannel(), true), message);
		}
	}

	private Multimap<ChannelHandlerContext, NodeUrn> getMulticastMapping(final Set<NodeUrn> nodeUrnsList) {

		final Multimap<ChannelHandlerContext, NodeUrn> mapping = HashMultimap.create();
		synchronized (contextToNodeUrnsMap) {
			for (ChannelHandlerContext ctx : contextToNodeUrnsMap.keySet()) {
				mapping.putAll(
						ctx,
						filter(contextToNodeUrnsMap.get(ctx), in(nodeUrnsList))
				);
			}
		}
		return mapping;
	}

	private HashSet<NodeUrn> toNodeUrnSet(final List<String> nodeUrnsList) {
		return newHashSet(
				transform(
						nodeUrnsList,
						STRING_TO_NODE_URN
				)
		);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		if (!(e.getMessage() instanceof Message)) {
			super.messageReceived(ctx, e);
		}

		final Message message = (Message) e.getMessage();
		switch (message.getType()) {

			case EVENT:
				final Event event = message.getEvent();
				log.trace("PortalChannelHandler.messageReceived(event={})", event);
				portalEventBus.post(event);
				sendEventAck(ctx, event);
				processEvent(ctx, event);
				break;

			case PROGRESS:
				final SingleNodeProgress progress = message.getProgress();
				log.trace("PortalChannelHandler.messageReceived(progress={})", progress);
				portalEventBus.post(progress);
				break;

			case RESPONSE:
				final SingleNodeResponse response = message.getResponse();
				log.trace("PortalChannelHandler.messageReceived({})", response);
				portalEventBus.post(response);
				break;

			default:
				throw new RuntimeException("Unexpected message type: " + message.getType());
		}
	}

	private void processEvent(final ChannelHandlerContext ctx, final Event event) {
		switch (event.getType()) {
			case DEVICES_ATTACHED:
				synchronized (contextToNodeUrnsMap) {
					for (String nodeUrnString : event.getDevicesAttachedEvent().getNodeUrnsList()) {
						contextToNodeUrnsMap.put(ctx, new NodeUrn(nodeUrnString));
					}
				}
				break;
			case DEVICES_DETACHED:
				synchronized (contextToNodeUrnsMap) {
					for (String nodeUrnString : event.getDevicesDetachedEvent().getNodeUrnsList()) {
						contextToNodeUrnsMap.get(ctx).remove(new NodeUrn(nodeUrnString));
					}
				}
				break;
		}
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("PortalChannelHandler.channelConnected(ctx={}, event={})", ctx, e);
		if (allChannels.isEmpty()) {
			log.debug("Subscribing to the event bus.");
			portalEventBus.register(this);
		}
		allChannels.add(e.getChannel());
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.trace("PortalChannelHandler.channelDisconnected(ctx={}, event={})", ctx, e);
		allChannels.remove(e.getChannel());
		if (allChannels.isEmpty()) {
			log.debug("Unsubscribing from the event bus.");
			portalEventBus.unregister(this);
		}
		super.channelDisconnected(ctx, e);
	}

	private void sendEventAck(final ChannelHandlerContext ctx, final Event event) {

		final EventAck.Builder eventAck = EventAck.newBuilder().setEventId(event.getEventId());
		final DefaultChannelFuture channelFuture = new DefaultChannelFuture(ctx.getChannel(), true);
		final Message message = Message.newBuilder().setType(Message.Type.EVENT_ACK).setEventAck(eventAck).build();

		write(ctx, channelFuture, message);
	}

	@Override
	public String toString() {
		return "PortalChannelHandler";
	}
}
