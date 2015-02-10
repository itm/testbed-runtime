package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.inject.Inject;
import com.google.protobuf.MessageLite;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayEventBus;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.GatewayFailureEvent;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CancellationException;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.util.concurrent.Futures.addCallback;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newEvent;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newMessage;

@SuppressWarnings("NullableProblems")
public class UpstreamMessageQueueImpl extends AbstractService implements UpstreamMessageQueue {

    private static Logger log = LoggerFactory.getLogger(UpstreamMessageQueueImpl.class);

    private final Object channelLock = new Object();
    private final UpstreamMessageQueueHelper queueHelper;
    private final GatewayEventBus gatewayEventBus;
    private final IdProvider idProvider;
    private Channel channel;
    private IBigQueue queue;
    private MultiClassSerializationHelper<MessageLite> serializationHelper;

    @Inject
    public UpstreamMessageQueueImpl(final UpstreamMessageQueueHelper queueHelper, final GatewayEventBus gatewayEventBus, final IdProvider idProvider) {
        this.queueHelper = queueHelper;
        this.gatewayEventBus = gatewayEventBus;
        this.idProvider = idProvider;
    }

    @Override
    protected void doStart() {
        log.trace("GatewayEventQueueImpl.doStart()");
        try {
            queue = queueHelper.createAndConfigureQueue();
        } catch (IOException e) {
            log.error("Failed to create event queue! Event persistence not available!", e);
            notifyFailed(e);
            return;
        }

        try {
            serializationHelper = queueHelper.configureEventSerializationHelper();
        } catch (Exception e) {
            log.error("Failed to configure serialization helper! Event persistence not available!", e);
            notifyFailed(e);
            return;
        }
        log.trace("GatewayEventQueueImpl configured successfully");


        gatewayEventBus.register(this);
        notifyStarted();
    }

    @Override
    protected void doStop() {
        log.trace("GatewayEventQueueImpl.doStop()");
        gatewayEventBus.unregister(this);
        notifyStopped();
    }

    @Override
    public void channelConnected(final Channel channel) {
        synchronized (channelLock) {
            this.channel = channel;
            addCallback(queue.dequeueAsync(), dequeueCallback);
        }
        log.trace("GatewayEventQueueImpl.channelConnected(): dequeue future callback added");
    }

    @Override
    public void channelDisconnected() {
        log.trace("UpstreamMessageQueueImpl.channelDisconnected()");
        synchronized (channelLock) {
            this.channel = null;
        }
    }

    @Override
    public void enqueue(Message message) {
        if (isPersistenceAvailable()) {
            try {
                byte[] serialization = serializationHelper.serialize(message);
                enqueue(serialization);
                log.trace("queue.enqueue");
            } catch (NotSerializableException e) {
                sendFailureEvent("The message " + message + " is not serializable. An appropriate serializer is missing!", e);
            }
        } else {
            sendFailureEvent("EventQueue was neither able to enqueue nor to send the message [" + message + "] - Queue: [" + queue + "], Helper: [" + serializationHelper + "]");
        }
    }

    private void enqueue(byte[] serializedMessage) {
        try {
            queue.enqueue(serializedMessage);
        } catch (IOException e) {
            sendFailureEvent("Failed to enqueue message in upstream message queue.", e);
        }
    }

    private void sendFailureEvent(String message, @Nullable Throwable cause) {
        gatewayEventBus.post(new GatewayFailureEvent(message, cause));
    }

    private void sendFailureEvent(String message) {
        sendFailureEvent(message, null);
    }

    private boolean isPersistenceAvailable() {
        return queue != null && serializationHelper != null;
    }

    private FutureCallback<byte[]> dequeueCallback = new FutureCallback<byte[]>() {
        @Override
        public void onSuccess(final byte[] dequeuedBytes) {

            assert serializationHelper != null; // otherwise this service should not be running

            synchronized (channelLock) {

                if (!isConnected()) {

                    log.trace("UpstreamMessageQueueImpl.dequeueCallback.onSuccess(): currently not connected; re-enqueuing.");
                    enqueue(dequeuedBytes);

                } else {

                    final Object message = serializationHelper.deserialize(dequeuedBytes);
                    log.trace("Forwarding message to portal server: {}", message);

                    ChannelFutureListener listener = new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                log.trace("Successfully forwarded message to portal, asynchronously dequeuing next message.");
                                addCallback(queue.dequeueAsync(), dequeueCallback);
                            } else {
                                log.error("Error forwarding message, re-enqueuing it and closing channel. Cause: {}", future.getCause());
                                if (future.getChannel() != null) {
                                    future.getChannel().close();
                                }
                                enqueue(dequeuedBytes);
                            }
                        }
                    };

                    try {

                        Channels.write(channel, message).addListener(listener);

                    } catch (Exception e) {
                        if (e instanceof ClosedChannelException) {
                            log.trace("ClosedChannelException while trying to forward message to portal server. Re-enqueuing message. Reason: {}", e.getMessage());
                            channelDisconnected();
                        }
                        log.error("Exception forwarding message to portal server. Closing channel. Cause: {}", e);
                        try {
                            Channels.close(channel);
                        } catch (Exception e1) {
                            log.error("Exception closing channel, faking close event. Cause: {}", e);
                            channelDisconnected();
                        }
                    }
                }

            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (!(t instanceof CancellationException)) {
                log.error("Dequeue future failed with throwable", t);
            } else {
                log.info("Dequeue future was canceled");
            }
        }
    };

    private boolean isConnected() {
        return channel != null;
    }

    @Subscribe
    public void onUpstreamMessageEvent(UpstreamMessageEvent upstreamMessageEvent) {
        enqueue(newMessage(newEvent(idProvider.get(), upstreamMessageEvent)));
    }

    @Subscribe
    public void onNotificationEvent(NotificationEvent notificationEvent) {
        enqueue(newMessage(newEvent(idProvider.get(), notificationEvent)));
    }

    @Subscribe
    public void onDevicesConnectedEvent(DevicesConnectedEvent event) {

        final de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent dae =
                de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent
                        .newBuilder()
                        .addAllNodeUrns(transform(event.getNodeUrns(), toStringFunction()))
                        .setTimestamp(now())
                        .build();

        enqueue(newMessage(newEvent(idProvider.get(), dae)));
    }

    @Subscribe
    public void onDevicesDisconnectedEvent(DevicesDisconnectedEvent event) {

        final de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent dde =
                de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent
                        .newBuilder()
                        .addAllNodeUrns(transform(event.getNodeUrns(), toStringFunction()))
                        .setTimestamp(now())
                        .build();

        enqueue(newMessage(newEvent(idProvider.get(), dde)));
    }

    @Subscribe
    public void onDevicesAttachedEvent(de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent devicesAttachedEvent) {
        enqueue(newMessage(newEvent(idProvider.get(), devicesAttachedEvent)));
    }

    @Subscribe
    public void onDevicesDetachedEvent(de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent devicesDetachedEvent) {
        enqueue(newMessage(newEvent(idProvider.get(), devicesDetachedEvent)));
    }

    @Subscribe
    public void onEvent(Event event) {

        switch (event.getType()) {

            // forward upstream events
            case DEVICES_ATTACHED:
            case DEVICES_DETACHED:
            case NOTIFICATION:
            case UPSTREAM_MESSAGE:
                enqueue(newMessage(event));
                break;

        }
    }

    @Subscribe
    public void onResponse(SingleNodeResponse singleNodeResponse) {
        enqueue(newMessage(singleNodeResponse));
    }

    @Subscribe
    public void onProgress(SingleNodeProgress singleNodeProgress) {
        enqueue(newMessage(singleNodeProgress));
    }

    @Subscribe
    public void onGetChannelPipelinesResponse(GetChannelPipelinesResponse response) {
        enqueue(newMessage(response));
    }

    private long now() {
        return new DateTime().getMillis();
    }
}
