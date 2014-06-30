package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;

public class UpstreamMessageQueueHelperImpl implements UpstreamMessageQueueHelper {
    private static final Function<MessageLite, byte[]> PROTOBUF_MESSAGE_SERIALIZER =
            new Function<MessageLite, byte[]>() {
                @Override
                public byte[] apply(final MessageLite input) {
                    return input.toByteArray();
                }
            };

    private final GatewayConfig gatewayConfig;

    @Inject
    public UpstreamMessageQueueHelperImpl(final GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public MultiClassSerializationHelper<MessageLite> configureEventSerializationHelper() throws IOException, ClassNotFoundException {
        Map<Class<? extends MessageLite>, Function<? extends MessageLite, byte[]>> serializers = newHashMap();
        Map<Class<? extends MessageLite>, Function<byte[], ? extends MessageLite>> deserializers = newHashMap();

        ///////////////////////// SERIALIZERS //////////////////////

        // events (upstream)

        serializers.put(Message.class, PROTOBUF_MESSAGE_SERIALIZER);


        ///////////////////////// DESERIALIZERS //////////////////////

        // events (upstream)

        deserializers.put(Message.class, new Deserializer(Message.getDefaultInstance()));


        String basePath = gatewayConfig.getEventQueuePath() + "/serializers";
        File mappingFile = new File(basePath + ".mapping");

        BiMap<Class<? extends MessageLite>, Byte> mapping = MultiClassSerializationHelper.<MessageLite>loadOrCreateClassByteMap(serializers, deserializers, mappingFile);
        return new MultiClassSerializationHelper<MessageLite>(serializers, deserializers, mapping);

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
