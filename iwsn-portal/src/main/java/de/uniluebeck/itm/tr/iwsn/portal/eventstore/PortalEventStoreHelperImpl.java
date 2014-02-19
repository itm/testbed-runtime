package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import eventstore.ChronicleBasedEventStore;
import eventstore.IEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
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
    public IEventStore createAndConfigureEventStore(String serializedReservationKey) throws FileNotFoundException {

        Map<Class<?>, Function<?, byte[]>> serializers = new HashMap<Class<?>, Function<?, byte[]>>();
        Map<Class<?>, Function<byte[], ?>> deserializers = new HashMap<Class<?>, Function<byte[], ?>>();
        serializers.put(Message.class, new Function<Message, byte[]>() {
            @Nullable
            @Override
            public byte[] apply(@Nullable Message message) {
                return message.toByteArray();
            }
        });

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

        deserializers.put(Message.class, new Function<byte[], Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable byte[] input) {
                Message message = Message.getDefaultInstance();
                try {
                    return message.newBuilderForType().mergeFrom(input).build();
                } catch (InvalidProtocolBufferException e) {
                    log.error("Can't deserialize protobuf message", e);
                    return null;
                }
            }
        });

        String baseName = new File(serializedReservationKey, portalServerConfig.getEventStorePath()).getAbsolutePath();
        return new ChronicleBasedEventStore(baseName, serializers, deserializers);
    }
}
