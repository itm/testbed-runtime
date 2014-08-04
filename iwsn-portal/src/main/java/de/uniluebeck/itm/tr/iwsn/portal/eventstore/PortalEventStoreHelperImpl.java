package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.eventstore.EventStoreFactory;
import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;

public class PortalEventStoreHelperImpl implements PortalEventStoreHelper {

    private static final Logger log = LoggerFactory.getLogger(PortalEventStoreHelperImpl.class);

    private static final Function<MessageLite, byte[]> PROTOBUF_MESSAGE_SERIALIZER =
            new Function<MessageLite, byte[]>() {
                @Override
                public byte[] apply(final MessageLite input) {
                    return input.toByteArray();
                }
            };

    private final PortalServerConfig portalServerConfig;

    @Inject
    public PortalEventStoreHelperImpl(final PortalServerConfig portalServerConfig) {
        this.portalServerConfig = portalServerConfig;
    }

    @Override
    public IEventStore createAndConfigureEventStore(String baseName)
            throws IOException, ClassNotFoundException {
        log.trace("PortalEventStoreHelperImpl.createAndConfigureEventStore({})", baseName);
        return configureEventStore(baseName, false);
    }

    @Override
    public IEventStore loadEventStore(String baseName, final boolean readOnly) {
        log.trace("PortalEventStoreHelperImpl.loadEventStore(res={}, readOnly={})", baseName, readOnly);
        try {
            return configureEventStore(baseName, readOnly);
        } catch (IOException e) {
            throw new InvalidParameterException(
                    "Failed to load event store for reservation " + baseName
            );
        } catch (ClassNotFoundException e) {
            log.error("Unable to load event store.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean eventStoreExistsForReservation(String baseName) {
        return new File(getEventStoreBasePathForBaseName(baseName) + ".data").exists();
    }

    private IEventStore configureEventStore(final String baseName, boolean readOnly)
            throws IOException, ClassNotFoundException {

        Map<Class<? extends MessageLite>, Function<? extends MessageLite, byte[]>> serializers = newHashMap();
        Map<Class<? extends MessageLite>, Function<byte[], ? extends MessageLite>> deserializers = newHashMap();

        ///////////////////////// SERIALIZERS //////////////////////

        // events (upstream)
        serializers.put(ReservationStartedEvent.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(ReservationEndedEvent.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(DevicesAttachedEvent.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(DevicesDetachedEvent.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(NotificationEvent.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(UpstreamMessageEvent.class, PROTOBUF_MESSAGE_SERIALIZER);

        // requests (downstream)
        serializers.put(Request.class, PROTOBUF_MESSAGE_SERIALIZER);

        // responses (upstream)
        serializers.put(SingleNodeResponse.class, PROTOBUF_MESSAGE_SERIALIZER);
        serializers.put(GetChannelPipelinesResponse.class, PROTOBUF_MESSAGE_SERIALIZER);

        ///////////////////////// DESERIALIZERS //////////////////////

        // events (upstream)
        deserializers.put(ReservationStartedEvent.class,
                new Deserializer(ReservationStartedEvent.getDefaultInstance())
        );
        deserializers.put(ReservationEndedEvent.class, new Deserializer(ReservationEndedEvent.getDefaultInstance()));
        deserializers.put(DevicesAttachedEvent.class, new Deserializer(DevicesAttachedEvent.getDefaultInstance()));
        deserializers.put(DevicesDetachedEvent.class, new Deserializer(DevicesDetachedEvent.getDefaultInstance()));
        deserializers.put(NotificationEvent.class, new Deserializer(NotificationEvent.getDefaultInstance()));
        deserializers.put(UpstreamMessageEvent.class, new Deserializer(UpstreamMessageEvent.getDefaultInstance()));

        // requests (downstream)
        deserializers.put(Request.class, new Deserializer(Request.getDefaultInstance()));

        // responses (upstream)
        deserializers.put(SingleNodeResponse.class, new Deserializer(SingleNodeResponse.getDefaultInstance()));
        deserializers.put(GetChannelPipelinesResponse.class,
                new Deserializer(GetChannelPipelinesResponse.getDefaultInstance())
        );

        String basePath = getEventStoreBasePathForBaseName(baseName);

        //noinspection unchecked
        return EventStoreFactory.create()
                .eventStoreWithBasePath(basePath)
                .withSerializers(serializers)
                .andDeserializers(deserializers)
                .inReadOnlyMode(readOnly)
                .havingMonotonicEventOrder(false).build();


    }


    private String getEventStoreBasePathForBaseName(String baseName) {
        return portalServerConfig.getEventStorePath() + "/" + baseName;
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
