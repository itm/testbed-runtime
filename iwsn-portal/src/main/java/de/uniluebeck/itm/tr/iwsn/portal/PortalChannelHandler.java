package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.*;

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.messages.MessageUtils.getUnconnectedStatusCode;
import static org.jboss.netty.channel.Channels.write;

public class PortalChannelHandler extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(PortalChannelHandler.class);

	private final PortalEventBus portalEventBus;

	private final MessageFactory messageFactory;

	private final Multimap<ChannelHandlerContext, NodeUrn> contextToNodeUrnsMap = HashMultimap.create();

	private final ChannelGroup allChannels = new DefaultChannelGroup();

	@Inject
	public PortalChannelHandler(final PortalEventBus portalEventBus, final MessageFactory messageFactory) {
		this.portalEventBus = portalEventBus;
		this.messageFactory = messageFactory;
	}

	public void doStart() {
		log.debug("Subscribing to the event bus.");
		portalEventBus.register(this);
	}

	public void doStop() {
		log.debug("Unsubscribing from the event bus.");
		portalEventBus.unregister(this);
	}

	@Subscribe
	public void on(final Object obj) {

		if (MessageHeaderPair.isUnwrappedMessageEvent(obj)) {

			final MessageHeaderPair pair = MessageHeaderPair.fromUnwrapped(obj);

			if (pair.header.getDownstream()) {

				final Set<NodeUrn> nodeUrns = newHashSet(transform(pair.header.getNodeUrnsList(), NodeUrn::new));
				final Multimap<ChannelHandlerContext, NodeUrn> mapping = getMulticastMapping(nodeUrns);
				final Map<ChannelHandlerContext, MessageHeaderPair> messagesToBeSent = newHashMap();

				Set<NodeUrn> gatewayNodeUrns;
				MessageHeaderPair gatewayPair;

				if (pair.header.getBroadcast()) {

					allChannels.write(pair.message);

				} else {

					for (ChannelHandlerContext ctx : mapping.keySet()) {

						gatewayNodeUrns = newHashSet(mapping.get(ctx));
						gatewayPair = messageFactory.split(pair, gatewayNodeUrns);

						messagesToBeSent.put(ctx, gatewayPair);
						sendMessages(messagesToBeSent);
						sendUnconnectedResponses(gatewayPair.header, getUnconnectedStatusCode(gatewayPair.header.getType()));
					}
				}
			}
		}
	}

	private void sendUnconnectedResponses(final Header header, final int statusCode) {

		final Set<NodeUrn> requestNodeUrns = newHashSet(transform(header.getNodeUrnsList(), NodeUrn::new));
		final Set<NodeUrn> unconnectedNodeUrns = getUnconnectedNodeUrns(requestNodeUrns);

		final Response response = messageFactory.response(
				header.hasSerializedReservationKey() ? Optional.of(header.getSerializedReservationKey()) : Optional.empty(),
				Optional.empty(),
				header.getType(),
				header.getCorrelationId(),
				unconnectedNodeUrns,
				statusCode,
				Optional.of("Node is not connected")
		);

		portalEventBus.post(response);
	}

	private void sendMessages(final Map<ChannelHandlerContext, MessageHeaderPair> messagesToBeSent) {

		for (Map.Entry<ChannelHandlerContext, MessageHeaderPair> entry : messagesToBeSent.entrySet()) {

			final ChannelHandlerContext ctx = entry.getKey();
			final MessageHeaderPair pair = entry.getValue();
			final MessageLite message = pair.message;

			final DefaultChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), true);
			future.addListener(channelFuture -> {
						//noinspection ThrowableResultOfMethodCallIgnored
						if (!channelFuture.isSuccess() && channelFuture.getCause() instanceof ClosedChannelException) {
							sendUnconnectedResponses(pair.header, -1);
						}
					}
			);
			Channels.write(ctx, future, message);
		}
	}

	private Set<NodeUrn> getUnconnectedNodeUrns(final Set<NodeUrn> nodeUrns) {

		final Set<NodeUrn> connectedNodeUrns = newHashSet();
		synchronized (contextToNodeUrnsMap) {
			for (ChannelHandlerContext key : contextToNodeUrnsMap.keys()) {
				connectedNodeUrns.addAll(contextToNodeUrnsMap.get(key));
			}
		}
		return Sets.difference(nodeUrns, connectedNodeUrns);
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

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		if (!MessageHeaderPair.isWrappedMessageEvent(e.getMessage())) {
			throw new IllegalStateException("Expected only messages of type " + Message.class.getCanonicalName()
					+ ", got " + e.getMessage().getClass().getCanonicalName()
					+ ". Probably the pipeline is misconfigured!");
		}

		final MessageHeaderPair pair = MessageHeaderPair.fromWrapped(e.getMessage());

		if (pair.header.getUpstream()) {

			process(ctx, pair);

			if (MessageHeaderPair.isEvent(pair.header.getType())) {
				sendEventAck(ctx, pair);
			}

			portalEventBus.post(pair.message);

		} else {
			log.warn("Received {} message from gateway that is not of type upstream", pair.header.getType());
		}
	}

	private void process(ChannelHandlerContext ctx, MessageHeaderPair pair) {

		List<NodeUrn> nodeUrns;

		switch (pair.header.getType()) {
			case EVENT_GATEWAY_CONNECTED:
				log.info("Gateway CONNECTED to portal server: {}", ctx.getChannel().getRemoteAddress().toString());
				break;
			case EVENT_GATEWAY_DISCONNECTED:
				log.info("Gateway DISCONNECTED from portal server: {}", ctx.getChannel().getRemoteAddress().toString());
				break;
			case EVENT_DEVICES_ATTACHED:
				nodeUrns = Lists.transform(pair.header.getNodeUrnsList(), NodeUrn::new);
				if (log.isInfoEnabled()) {
					log.info("Devices ATTACHED to gateway {}: {}", ctx.getChannel().getRemoteAddress().toString(), nodeUrns);
				}
				synchronized (contextToNodeUrnsMap) {
					contextToNodeUrnsMap.putAll(ctx, nodeUrns);
				}
				break;
			case EVENT_DEVICES_DETACHED:
				nodeUrns = Lists.transform(pair.header.getNodeUrnsList(), NodeUrn::new);
				if (log.isInfoEnabled()) {
					log.info("Devices DETACHED from gateway {}: {}", ctx.getChannel().getRemoteAddress().toString(), nodeUrns);
				}
				synchronized (contextToNodeUrnsMap) {
					contextToNodeUrnsMap.removeAll(nodeUrns);
				}
				break;
		}
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		log.debug("PortalChannelHandler.channelConnected(ctx={}, event={}): {}", ctx, e, ctx.getChannel().getRemoteAddress().toString());
		allChannels.add(ctx.getChannel());
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

		log.debug("PortalChannelHandler.channelDisconnected(ctx={}, event={}): {}", ctx, e, ctx.getChannel().getRemoteAddress().toString());

		allChannels.remove(ctx.getChannel());

		synchronized (contextToNodeUrnsMap) {

			final Collection<NodeUrn> nodeUrns = contextToNodeUrnsMap.get(ctx);

			String remoteAddress = ctx.getChannel().getRemoteAddress().toString();
			portalEventBus.post(messageFactory.gatewayDisconnectedEvent(Optional.empty(), remoteAddress, nodeUrns));
			log.info("Gateway DISCONNECTED from portal server: {}", remoteAddress);

			// send notification that devices on this channel have been detached
			if (!nodeUrns.isEmpty()) {
				portalEventBus.post(messageFactory.devicesDetachedEvent(Optional.empty(), nodeUrns));
			}

			contextToNodeUrnsMap.removeAll(ctx);
		}

		super.channelDisconnected(ctx, e);
	}

	private void sendEventAck(final ChannelHandlerContext ctx, final MessageHeaderPair pair) {
		final EventAck ack = messageFactory.eventAck(pair.header, Optional.empty());
		write(ctx, new DefaultChannelFuture(ctx.getChannel(), true), ack);
	}

	@Override
	public String toString() {
		return "PortalChannelHandler";
	}
}
