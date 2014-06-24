package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.eventstore.helper.EventStoreSerializationHelper;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;

public class GatewayEventQueueHelperImpl implements GatewayEventQueueHelper {
    private static final Function<MessageLite, byte[]> PROTOBUF_MESSAGE_SERIALIZER =
            new Function<MessageLite, byte[]>() {
                @Override
                public byte[] apply(final MessageLite input) {
                    return input.toByteArray();
                }
            };

    private final GatewayConfig gatewayConfig;

    @Inject
    public GatewayEventQueueHelperImpl(final GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public EventStoreSerializationHelper configureEventSerializationHelper() throws FileNotFoundException, ClassNotFoundException {
        Map<Class<? extends MessageLite>, Function<? extends MessageLite, byte[]>> serializers = newHashMap();
        Map<Class<? extends MessageLite>, Function<byte[], ? extends MessageLite>> deserializers = newHashMap();

        ///////////////////////// SERIALIZERS //////////////////////

        // events (upstream)
        serializers.put(DevicesAttachedEvent.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(DevicesDetachedEvent.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(NotificationEvent.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(UpstreamMessageEvent.class, PROTOBUF_MESSAGE_SERIALIZER);


        ///////////////////////// DESERIALIZERS //////////////////////

        // events (upstream)
        deserializers.put(DevicesAttachedEvent.class, new Deserializer(DevicesAttachedEvent.getDefaultInstance()));
        deserializers.put(DevicesDetachedEvent.class, new Deserializer(DevicesDetachedEvent.getDefaultInstance()));
        deserializers.put(NotificationEvent.class, new Deserializer(NotificationEvent.getDefaultInstance()));
        deserializers.put(UpstreamMessageEvent.class, new Deserializer(UpstreamMessageEvent.getDefaultInstance()));


        String basePath = gatewayConfig.getEventQueuePath() + "/serializers";
        return new EventStoreSerializationHelper(basePath, serializers, deserializers);

    }

    @Override
    public IBigQueue createAndConfigureQueue() throws IOException {
        return new BigQueueImpl(gatewayConfig.getEventQueuePath(), "event-queue");
    }

    private class Deserializer implements Function<byte[], MessageLite> {

        private final MessageLite defaultInstance;

        public Deserializer(final MessageLite defaultInstance) {
            this.defaultInstance = defaultInstance;
        }

        @Override
        public MessageLite apply(final byte[] input) {
            try {
                return defaultInstance.newBuilderForType().mergeFrom(input).build();
            } catch (InvalidProtocolBufferException e) {
                throw propagate(e);
            }
        }
    }
}
