package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import de.uniluebeck.itm.tr.iwsn.portal.externalplugins.ExternalPluginService;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.channels.ClosedChannelException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.jboss.netty.channel.Channels.write;

public class PortalChannelHandler extends SimpleChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(PortalChannelHandler.class);

    private final PortalEventBus portalEventBus;

    private final IdProvider idProvider;

    private final ExternalPluginService externalPluginService;

    private final Multimap<ChannelHandlerContext, NodeUrn> contextToNodeUrnsMap = HashMultimap.create();

    private final ChannelGroup allChannels = new DefaultChannelGroup();

    @Inject
    public PortalChannelHandler(final PortalEventBus portalEventBus,
                                final IdProvider idProvider,
                                final ExternalPluginService externalPluginService) {
        this.portalEventBus = portalEventBus;
        this.idProvider = idProvider;
        this.externalPluginService = externalPluginService;
    }

    public void doStart() {
        log.debug("Subscribing to the event bus.");
        portalEventBus.register(this);
        externalPluginService.startAsync().awaitRunning();
    }

    public void doStop() {
        externalPluginService.stopAsync().awaitTerminated();
        log.debug("Unsubscribing from the event bus.");
        portalEventBus.unregister(this);
    }

	@Subscribe
	public void on(final AreNodesAliveRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final AreNodesConnectedRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final DisableNodesRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final DisableVirtualLinksRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final DisablePhysicalLinksRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final EnableNodesRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final EnablePhysicalLinksRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final EnableVirtualLinksRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final FlashImagesRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final GetChannelPipelinesRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final ResetNodesRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final SendDownstreamMessagesRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final SetChannelPipelinesRequest message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final Progress message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final Response message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final GetChannelPipelinesResponse message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final UpstreamMessageEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final DevicesAttachedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final DevicesDetachedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final GatewayConnectedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final GatewayDisconnectedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final NotificationEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final ReservationStartedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final ReservationEndedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final ReservationMadeEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final ReservationCancelledEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final ReservationOpenedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final ReservationClosedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final ReservationFinalizedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final DeviceConfigCreatedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final DeviceConfigUpdatedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final DeviceConfigDeletedEvent message) {
		forward(message, message.getHeader());
	}

	@Subscribe
	public void on(final EventAck message) {
		klajflkdsajflkasj
		forward(message, message.getHeader());
	}

	private void forward(final MessageLite message, EventHeader header) {

		if (header.getDownstream()) {
			sendToExternalPlugins(message);
			sendToGateways(message);
		}

		if (header.getUpstream()) {
			portalEventBus.post(message);
		}
	}

    private void forward(final MessageLite message, RequestResponseHeader header) {

		if (header.getDownstream()) {
			sendToExternalPlugins(message);
			sendToGateways(message);
		}

		if (header.getUpstream()) {
			portalEventBus.post(message);
		}
	}

    @Subscribe
    public void onRequest(final Request request) {

        sendToExternalPlugins(request);

        final String reservationId = request.hasReservationId() ? request.getReservationId() : null;
        final long requestId = request.getRequestId();
        final Set<NodeUrn> nodeUrns;
        final Multimap<ChannelHandlerContext, NodeUrn> mapping;
        final Map<ChannelHandlerContext, Request> requestsToBeSent = newHashMap();
        final List<Link> links;

        switch (request.getType()) {

            case ARE_NODES_ALIVE:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    requestsToBeSent.put(ctx, newAreNodesAliveRequest(reservationId, requestId, mapping.get(ctx)));
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, 0, getUnconnectedNodeUrns(nodeUrns));
                break;

            case ARE_NODES_CONNECTED:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    requestsToBeSent.put(ctx, newAreNodesConnectedRequest(reservationId, requestId, mapping.get(ctx)));
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, 0, getUnconnectedNodeUrns(nodeUrns));
                break;

            case DISABLE_NODES:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    requestsToBeSent.put(ctx, newDisableNodesRequest(reservationId, requestId, mapping.get(ctx)));
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case DISABLE_PHYSICAL_LINKS:
                links = request.getDisablePhysicalLinksRequest().getLinksList();
                nodeUrns = getNodeUrns(request);
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
                    requestsToBeSent.put(
                            ctx,
                            newDisablePhysicalLinksRequest(reservationId, requestId, subRequestLinks)
                    );
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case DISABLE_VIRTUAL_LINKS:
                links = request.getDisableVirtualLinksRequest().getLinksList();
                nodeUrns = getNodeUrns(request);
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
                    requestsToBeSent.put(ctx, newDisableVirtualLinksRequest(reservationId, requestId, subRequestLinks));
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case ENABLE_NODES:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    requestsToBeSent.put(ctx, newEnableNodesRequest(reservationId, requestId, mapping.get(ctx)));
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case ENABLE_PHYSICAL_LINKS:
                links = request.getEnablePhysicalLinksRequest().getLinksList();
                nodeUrns = getNodeUrns(request);
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
                    requestsToBeSent.put(ctx, newEnablePhysicalLinksRequest(reservationId, requestId, subRequestLinks));
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case ENABLE_VIRTUAL_LINKS:
                links = request.getEnableVirtualLinksRequest().getLinksList();
                nodeUrns = getNodeUrns(request);
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
                    requestsToBeSent.put(ctx, newEnableVirtualLinksRequest(reservationId, requestId, subRequestLinks));
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case FLASH_IMAGES:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    final ByteString imageBytes = request.getFlashImagesRequest().getImage();
                    requestsToBeSent.put(
                            ctx,
                            newFlashImagesRequest(reservationId, requestId, mapping.get(ctx), imageBytes)
                    );
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case GET_CHANNEL_PIPELINES:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    requestsToBeSent.put(
                            ctx,
                            newGetChannelPipelinesRequest(reservationId, requestId, mapping.get(ctx))
                    );
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case RESET_NODES:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    requestsToBeSent.put(ctx, newResetNodesRequest(reservationId, requestId, mapping.get(ctx)));
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case SEND_DOWNSTREAM_MESSAGES:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    final ByteString bytes = request.getSendDownstreamMessagesRequest().getMessageBytes();
                    requestsToBeSent.put(
                            ctx,
                            newSendDownstreamMessageRequest(reservationId, requestId, mapping.get(ctx), bytes)
                    );
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            case SET_CHANNEL_PIPELINES:
                nodeUrns = getNodeUrns(request);
                mapping = getMulticastMapping(nodeUrns);

                for (ChannelHandlerContext ctx : mapping.keySet()) {
                    final List<ChannelHandlerConfiguration> configs =
                            request.getSetChannelPipelinesRequest().getChannelHandlerConfigurationsList();
                    requestsToBeSent.put(
                            ctx,
                            newSetChannelPipelinesRequest(reservationId, requestId, mapping.get(ctx), configs)
                    );
                }

                sendRequests(requestsToBeSent);
                sendUnconnectedResponses(reservationId, requestId, -1, getUnconnectedNodeUrns(nodeUrns));
                break;

            default:
                throw new RuntimeException("Unknown request type received!");
        }
    }

    private void sendUnconnectedResponses(final String reservationId, final long requestId, final int statusCode,
                                          final Iterable<NodeUrn> unconnectedNodeUrns) {

        for (NodeUrn nodeUrn : unconnectedNodeUrns) {

            final SingleNodeResponse response = newSingleNodeResponse(
                    reservationId,
                    requestId,
                    nodeUrn,
                    statusCode,
                    "Node is not connected"
            );

            portalEventBus.post(response);
            sendToExternalPlugins(response);
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
            final DefaultChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), true);
            future.addListener(new ChannelFutureListener() {
                                   @Override
                                   public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                                       //noinspection ThrowableResultOfMethodCallIgnored
                                       if (!channelFuture.isSuccess() && channelFuture
                                               .getCause() instanceof ClosedChannelException) {
                                           sendUnconnectedResponses(
                                                   requestBuilder.getReservationId(),
                                                   requestBuilder.getRequestId(),
                                                   -1,
                                                   extractNodeUrns(requestBuilder)
                                           );
                                       }
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

        if (!(e.getMessage() instanceof Message)) {
            super.messageReceived(ctx, e);
        }

        final Message message = (Message) e.getMessage();
        switch (message.getType()) {

            case EVENT:
                final Event event = message.getEvent();
                sendToExternalPlugins(event);
                log.trace("PortalChannelHandler.messageReceived(event={})", event);
                switch (event.getType()) {
                    case GATEWAY_CONNECTED:
                        portalEventBus.post(event.getGatewayConnectedEvent());
                        break;
                    case DEVICES_ATTACHED:
                        portalEventBus.post(event.getDevicesAttachedEvent());
                        break;
                    case DEVICES_DETACHED:
                        portalEventBus.post(event.getDevicesDetachedEvent());
                        break;
                    case NOTIFICATION:
                        portalEventBus.post(event.getNotificationEvent());
                        break;
                    case UPSTREAM_MESSAGE:
                        portalEventBus.post(event.getUpstreamMessageEvent());
                        break;
                }
                sendEventAck(ctx, event);
                processEvent(ctx, event);
                break;

            case PROGRESS:
                final SingleNodeProgress progress = message.getProgress();
                log.trace("PortalChannelHandler.messageReceived(progress={})", progress);
                portalEventBus.post(progress);
                sendToExternalPlugins(progress);
                break;

            case RESPONSE:
                final SingleNodeResponse response = message.getResponse();
                log.trace("PortalChannelHandler.messageReceived({})", response);
                portalEventBus.post(response);
                sendToExternalPlugins(response);
                break;

            case GET_CHANNELPIPELINES_RESPONSE:
                final GetChannelPipelinesResponse getChannelPipelinesResponse =
                        message.getGetChannelPipelinesResponse();
                log.trace("PortalChannelHandler.messageReceived({})", getChannelPipelinesResponse);
                portalEventBus.post(getChannelPipelinesResponse);
                sendToExternalPlugins(getChannelPipelinesResponse);
                break;

            case KEEP_ALIVE:
            case KEEP_ALIVE_ACK:
                break;

            default:
                throw new RuntimeException("Unexpected message type: " + message.getType());
        }
    }

    private void processEvent(final ChannelHandlerContext ctx, final Event event) {
        List<String> nodeUrns;
        switch (event.getType()) {
            case GATEWAY_CONNECTED:
                log.info("Gateway CONNECTED to portal server: {}", ctx.getChannel().getRemoteAddress().toString());
                break;
            case DEVICES_ATTACHED:
                nodeUrns = event.getDevicesAttachedEvent().getNodeUrnsList();
                if (log.isInfoEnabled()) {
                    log.info("Devices ATTACHED to gateway {}: {}", ctx.getChannel().getRemoteAddress().toString(), nodeUrns);
                }
                synchronized (contextToNodeUrnsMap) {
                    for (String nodeUrnString : nodeUrns) {
                        contextToNodeUrnsMap.put(ctx, new NodeUrn(nodeUrnString));
                    }
                }
                break;
            case DEVICES_DETACHED:
                nodeUrns = event.getDevicesDetachedEvent().getNodeUrnsList();
                if (log.isInfoEnabled()) {
                    log.info("Devices DETACHED from gateway {}: {}", ctx.getChannel().getRemoteAddress().toString(), nodeUrns);
                }
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
            portalEventBus.post(MessageFactoryImpl.newGatewayDisconnectedEvent(remoteAddress, nodeUrns));
            log.info("Gateway DISCONNECTED from portal server: {}", remoteAddress);

            // send notification that devices on this channel have been detached
            if (!nodeUrns.isEmpty()) {
                portalEventBus.post(newDevicesDetachedEvent(nodeUrns));
            }

            contextToNodeUrnsMap.removeAll(ctx);
        }

        super.channelDisconnected(ctx, e);
    }

    private void sendEventAck(final ChannelHandlerContext ctx, final Event event) {

        final EventAck eventAck = EventAck.newBuilder().setEventId(event.getEventId()).build();
        final DefaultChannelFuture channelFuture = new DefaultChannelFuture(ctx.getChannel(), true);
        final Message message = Message.newBuilder().setType(Message.Type.EVENT_ACK).setEventAck(eventAck).build();

        sendToExternalPlugins(eventAck);

        write(ctx, channelFuture, message);
    }

    private void sendToGateways(final MessageLite message) {
        allChannels.write(message);
    }

    @Override
    public String toString() {
        return "PortalChannelHandler";
    }
}
