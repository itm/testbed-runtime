package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
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

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
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

		final List<String> nodeUrnsList;
		final Set<NodeUrn> nodeUrns;
		final Multimap<ChannelHandlerContext, NodeUrn> mapping;
		final Map<ChannelHandlerContext, Request.Builder> requestsToBeSent = newHashMap();

		switch (request.getType()) {

			case ARE_NODES_ALIVE:

				nodeUrnsList = request.getAreNodesAliveRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {

					final AreNodesAliveRequest.Builder areNodesAliveRequestBuilder = AreNodesAliveRequest.newBuilder()
							.addAllNodeUrns(transform(mapping.get(ctx), toStringFunction()));

					final Request.Builder requestBuilder = Request.newBuilder()
							.setRequestId(request.getRequestId())
							.setType(Request.Type.ARE_NODES_ALIVE)
							.setAreNodesAliveRequest(areNodesAliveRequestBuilder);

					requestsToBeSent.put(ctx, requestBuilder);
				}
				sendRequests(requestsToBeSent);
				break;

			case ARE_NODES_CONNECTED:

				nodeUrnsList = request.getAreNodesConnectedRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {

					final AreNodesConnectedRequest.Builder areNodesConnectedRequestBuilder = AreNodesConnectedRequest
							.newBuilder()
							.addAllNodeUrns(transform(mapping.get(ctx), toStringFunction()));

					final Request.Builder requestBuilder = Request.newBuilder()
							.setRequestId(request.getRequestId())
							.setType(Request.Type.ARE_NODES_CONNECTED)
							.setAreNodesConnectedRequest(areNodesConnectedRequestBuilder);

					requestsToBeSent.put(ctx, requestBuilder);
				}
				sendRequests(requestsToBeSent);
				break;

			case DESTROY_VIRTUAL_LINK:
				throw new RuntimeException("TODO: not yet implemented");

			case DISABLE_NODE:

				nodeUrnsList = request.getDisableNodesRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {

					final DisableNodesRequest.Builder disableNodesRequestBuilder = DisableNodesRequest
							.newBuilder()
							.addAllNodeUrns(transform(mapping.get(ctx), toStringFunction()));

					final Request.Builder requestBuilder = Request.newBuilder()
							.setRequestId(request.getRequestId())
							.setType(Request.Type.DISABLE_NODE)
							.setDisableNodesRequest(disableNodesRequestBuilder);

					requestsToBeSent.put(ctx, requestBuilder);
				}
				sendRequests(requestsToBeSent);
				break;

			case DISABLE_PHYSICAL_LINK:
				throw new RuntimeException("TODO: not yet implemented");

			case ENABLE_NODE:

				nodeUrnsList = request.getEnableNodesRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {

					final EnableNodesRequest.Builder enableNodesRequestBuilder = EnableNodesRequest
							.newBuilder()
							.addAllNodeUrns(transform(mapping.get(ctx), toStringFunction()));

					final Request.Builder requestBuilder = Request.newBuilder()
							.setRequestId(request.getRequestId())
							.setType(Request.Type.ENABLE_NODE)
							.setEnableNodesRequest(enableNodesRequestBuilder);

					requestsToBeSent.put(ctx, requestBuilder);
				}
				sendRequests(requestsToBeSent);
				break;

			case ENABLE_PHYSICAL_LINK:
				throw new RuntimeException("TODO: not yet implemented");

			case FLASH_DEFAULT_IMAGE:
				throw new RuntimeException("TODO: remove");

			case FLASH_PROGRAMS:
				throw new RuntimeException("TODO: not yet implemented");

			case RESET_NODES:

				nodeUrnsList = request.getResetNodesRequest().getNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {

					final ResetNodesRequest.Builder resetNodesRequestBuilder = ResetNodesRequest
							.newBuilder()
							.addAllNodeUrns(transform(mapping.get(ctx), toStringFunction()));

					final Request.Builder requestBuilder = Request.newBuilder()
							.setRequestId(request.getRequestId())
							.setType(Request.Type.ENABLE_NODE)
							.setResetNodesRequest(resetNodesRequestBuilder);

					requestsToBeSent.put(ctx, requestBuilder);
				}
				sendRequests(requestsToBeSent);
				break;

			case SEND_DOWNSTREAM_MESSAGE:

				nodeUrnsList = request.getSendDownstreamMessageRequest().getTargetNodeUrnsList();
				nodeUrns = toNodeUrnSet(nodeUrnsList);
				mapping = getMulticastMapping(nodeUrns);

				for (ChannelHandlerContext ctx : mapping.keySet()) {

					final SendDownstreamMessageRequest.Builder sendDownstreamMessageRequestBuilder =
							SendDownstreamMessageRequest
									.newBuilder()
									.addAllTargetNodeUrns(transform(mapping.get(ctx), toStringFunction()))
									.setMessageBytes(request.getSendDownstreamMessageRequest().getMessageBytes());

					final Request.Builder requestBuilder = Request.newBuilder()
							.setRequestId(request.getRequestId())
							.setType(Request.Type.SEND_DOWNSTREAM_MESSAGE)
							.setSendDownstreamMessageRequest(sendDownstreamMessageRequestBuilder);

					requestsToBeSent.put(ctx, requestBuilder);
				}
				sendRequests(requestsToBeSent);
				break;

			case SET_CHANNEL_PIPELINE:
				throw new RuntimeException("TODO: not yet implemented");

			case SET_DEFAULT_CHANNEL_PIPELINE:
				throw new RuntimeException("TODO: remove");

			case SET_VIRTUAL_LINK:
				throw new RuntimeException("TODO: not yet implemented");
		}
	}

	private void sendRequests(final Map<ChannelHandlerContext, Request.Builder> requestsToBeSent) {
		for (Map.Entry<ChannelHandlerContext, Request.Builder> entry : requestsToBeSent.entrySet()) {
			final ChannelHandlerContext ctx = entry.getKey();
			final Request.Builder requestBuilder = entry.getValue();
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
				portalEventBus.post(event);
				sendEventAck(ctx, event);
				processEvent(ctx, event);
				break;
			case PROGRESS:
				portalEventBus.post(message.getProgress());
				break;
			case RESPONSE:
				portalEventBus.post(message.getResponse());
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
		log.debug("Channel connected: {}", e);
		if (allChannels.isEmpty()) {
			log.debug("Subscribing to the event bus.");
			portalEventBus.register(this);
		}
		allChannels.add(e.getChannel());
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.debug("Channel disconnected: {}", e);
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
}
