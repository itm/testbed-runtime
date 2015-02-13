package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.iwsn.gateway.eventqueue.UpstreamMessageQueue;
import de.uniluebeck.itm.tr.iwsn.messages.Event;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;

public class GatewayChannelHandler extends SimpleChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayChannelHandler.class);

    private final GatewayEventBus gatewayEventBus;

    private final IdProvider idProvider;

    private final DeviceManager deviceManager;

    private final UpstreamMessageQueue upstreamMessageQueue;

    private Channel channel;

    @Inject
    public GatewayChannelHandler(final GatewayEventBus gatewayEventBus,
                                 final IdProvider idProvider,
                                 final DeviceManager deviceManager,
                                 final UpstreamMessageQueue upstreamMessageQueue) {
        this.gatewayEventBus = gatewayEventBus;
        this.idProvider = idProvider;
        this.deviceManager = deviceManager;
        this.upstreamMessageQueue = upstreamMessageQueue;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

        if (!(e.getMessage() instanceof Message)) {
            super.messageReceived(ctx, e);
        }

        final Message message = (Message) e.getMessage();
        switch (message.getType()) {
            case REQUEST:
                gatewayEventBus.post(((Message) e.getMessage()).getRequest());
                break;
            case EVENT_ACK:
                gatewayEventBus.post(((Message) e.getMessage()).getEventAck());
                break;
            case EVENT:
                gatewayEventBus.post(((Message) e.getMessage()).getEvent());
            case KEEP_ALIVE:
            case KEEP_ALIVE_ACK:
                break;
            default:
                throw new RuntimeException("Unexpected message type: " + message.getType());
        }
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

        log.trace("GatewayChannelHandler.channelConnected(ctx={}, event={})", ctx, e);

        if (!upstreamMessageQueue.isRunning()) {
            upstreamMessageQueue.awaitRunning(5, TimeUnit.SECONDS);
            if (!upstreamMessageQueue.isRunning()) {
                throw new RuntimeException("GatewayChannelHandler.channelConnected(): UpstreamMessageQueue should be running. Timed out waiting for it!");
            }
        }

        gatewayEventBus.register(this);

        channel = e.getChannel();
        upstreamMessageQueue.channelConnected(channel);

        final String hostname = InetAddress.getLocalHost().getHostAddress();
		upstreamMessageQueue.enqueue(newMessage(newEvent(idProvider.get(), newGatewayConnectedEvent(hostname))));

		final Set<NodeUrn> connectedNodeUrns = deviceManager.getConnectedNodeUrns();

        if (!connectedNodeUrns.isEmpty()) {
            upstreamMessageQueue.enqueue(newMessage(newEvent(idProvider.get(), newDevicesAttachedEvent(connectedNodeUrns))));
        }

        super.channelConnected(ctx, e);
    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        log.trace("GatewayChannelHandler.channelDisconnected(ctx={}, event={}", ctx, e);
        gatewayEventBus.unregister(this);
        upstreamMessageQueue.channelDisconnected();
        channel = null;
        super.channelDisconnected(ctx, e);
    }


    @Subscribe
    public void onEvent(Event event) {

        switch (event.getType()) {
            // unwrap downstream events and re-post
            case DEVICE_CONFIG_CREATED:
                gatewayEventBus.post(event.getDeviceConfigCreatedEvent());
                break;
            case DEVICE_CONFIG_DELETED:
                gatewayEventBus.post(event.getDeviceConfigDeletedEvent());
                break;
            case DEVICE_CONFIG_UPDATED:
                gatewayEventBus.post(event.getDeviceConfigUpdatedEvent());
                break;
            case RESERVATION_CANCELLED:
                gatewayEventBus.post(event.getReservationCancelledEvent());
                break;
            case RESERVATION_ENDED:
                gatewayEventBus.post(event.getReservationEndedEvent());
                break;
            case RESERVATION_MADE:
                gatewayEventBus.post(event.getReservationMadeEvent());
                break;
            case RESERVATION_STARTED:
                gatewayEventBus.post(event.getReservationStartedEvent());
                break;
        }
    }

    @Override
    public String toString() {
        return "GatewayChannelHandler";
    }
}
