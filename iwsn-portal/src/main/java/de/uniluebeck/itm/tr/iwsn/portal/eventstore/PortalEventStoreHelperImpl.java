package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import eventstore.ChronicleBasedEventStore;
import eventstore.IEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class PortalEventStoreHelperImpl implements PortalEventStoreHelper {

    private static final Logger log = LoggerFactory.getLogger(PortalEventStoreHelperImpl.class);
    private final ReservationManager reservationManager;
    private final PortalServerConfig portalServerConfig;

    @Inject
    public PortalEventStoreHelperImpl(final ReservationManager reservationManager, final PortalServerConfig portalServerConfig) {
        this.reservationManager = reservationManager;
        this.portalServerConfig = portalServerConfig;
    }

    @Override
    public IEventStore createAndConfigureEventStore(final String serializedReservationKey) throws FileNotFoundException {

        Map<Class<?>, Function<?, byte[]>> serializers = new HashMap<Class<?>, Function<?, byte[]>>();
        Map<Class<?>, Function<byte[], ?>> deserializers = new HashMap<Class<?>, Function<byte[], ?>>();

        Function<MessageLite, byte[]> messageSerializer = new Function<MessageLite, byte[]>() {
            @Nullable
            @Override
            public byte[] apply(@Nullable MessageLite input) {
                return input.toByteArray();
            }
        };

        serializers.put(DevicesAttachedEvent.class, messageSerializer);
        serializers.put(DevicesDetachedEvent.class, messageSerializer);
        serializers.put(UpstreamMessageEvent.class, messageSerializer);
        serializers.put(NotificationEvent.class, messageSerializer);
        serializers.put(SingleNodeResponse.class, messageSerializer);
        serializers.put(GetChannelPipelinesResponse.class, messageSerializer);
        serializers.put(Request.class, messageSerializer);

        serializers.put(ReservationStartedEvent.class, new Function<ReservationStartedEvent, byte[]>() {
            @Nullable
            @Override
            public byte[] apply(@Nullable ReservationStartedEvent input) {
                return input.getReservation().getSerializedKey().getBytes();
            }
        });

        serializers.put(ReservationEndedEvent.class, new Function<ReservationEndedEvent, byte[]>() {
            @Nullable
            @Override
            public byte[] apply(@Nullable ReservationEndedEvent input) {
                return input.getReservation().getSerializedKey().getBytes();
            }
        });

        deserializers.put(ReservationStartedEvent.class, new Function<byte[], ReservationStartedEvent>() {
            @Nullable
            @Override
            public ReservationStartedEvent apply(@Nullable byte[] input) {
                try {
                    String json = new String(input, "UTF-8");
                    return new ReservationStartedEvent(reservationManager.getReservation(json));
                } catch (Exception e) {
                    log.error("Can't deserialize the ReservationStartedEvent", e);
                    return null;
                }

            }
        });

        deserializers.put(ReservationEndedEvent.class, new Function<byte[], ReservationEndedEvent>() {
            @Nullable
            @Override
            public ReservationEndedEvent apply(@Nullable byte[] input) {
                try {
                    String json = new String(input, "UTF-8");
                    return new ReservationEndedEvent(reservationManager.getReservation(json));
                } catch (Exception e) {
                    log.error("Can't deserialize the ReservationEndedEvent", e);
                    return null;
                }

            }
        });

        deserializers.put(DevicesAttachedEvent.class, new Function<byte[], DevicesAttachedEvent>() {
            @Nullable
            @Override
            public DevicesAttachedEvent apply(@Nullable byte[] input) {
                try {
                    return DevicesAttachedEvent.getDefaultInstance().parseFrom(input);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Can't deserialize event");
                    return null;
                }
            }
        });

        deserializers.put(DevicesDetachedEvent.class, new Function<byte[], DevicesDetachedEvent>() {
            @Nullable
            @Override
            public DevicesDetachedEvent apply(@Nullable byte[] input) {
                try {
                    return DevicesDetachedEvent.getDefaultInstance().parseFrom(input);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Can't deserialize event");
                    return null;
                }
            }
        });

        deserializers.put(UpstreamMessageEvent.class, new Function<byte[], UpstreamMessageEvent>() {
            @Nullable
            @Override
            public UpstreamMessageEvent apply(@Nullable byte[] input) {
                try {
                    return UpstreamMessageEvent.getDefaultInstance().parseFrom(input);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Can't deserialize event");
                    return null;
                }
            }
        });


        deserializers.put(NotificationEvent.class, new Function<byte[], NotificationEvent>() {
            @Nullable
            @Override
            public NotificationEvent apply(@Nullable byte[] input) {
                try {
                    return NotificationEvent.getDefaultInstance().parseFrom(input);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Can't deserialize event");
                    return null;
                }
            }
        });



        deserializers.put(SingleNodeResponse.class, new Function<byte[], SingleNodeResponse>() {
            @Nullable
            @Override
            public SingleNodeResponse apply(@Nullable byte[] input) {
                try {
                    return SingleNodeResponse.getDefaultInstance().parseFrom(input);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Can't deserialize event");
                    return null;
                }
            }
        });

        deserializers.put(GetChannelPipelinesResponse.class, new Function<byte[], GetChannelPipelinesResponse>() {
            @Nullable
            @Override
            public GetChannelPipelinesResponse apply(@Nullable byte[] input) {
                try {
                    return GetChannelPipelinesResponse.getDefaultInstance().parseFrom(input);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Can't deserialize event");
                    return null;
                }
            }
        });

        deserializers.put(Request.class, new Function<byte[], Request>() {
            @Nullable
            @Override
            public Request apply(@Nullable byte[] input) {
                try {
                    return Request.getDefaultInstance().parseFrom(input);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Can't deserialize event");
                    return null;
                }
            }
        });

        String baseName = eventstoreBasenameForReservation(serializedReservationKey);
        log.trace("Creating new chronicle at {}", baseName);
        return new ChronicleBasedEventStore(baseName, serializers, deserializers);
    }

    @Override
    public String eventstoreBasenameForReservation(String serializedReservationKey) {
        return portalServerConfig.getEventStorePath() +"/"+serializedReservationKey;
    }
}
