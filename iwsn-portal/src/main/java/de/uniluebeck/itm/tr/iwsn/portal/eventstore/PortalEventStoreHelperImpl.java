package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.eventstore.EventStoreFactory;
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

	private static final Function<Message, byte[]> SERIALIZER = MessageLite::toByteArray;

	private static final Function<byte[], Message> DESERIALIZER = (bytes) -> {
		try {
			return Message.getDefaultInstance().newBuilderForType().mergeFrom(bytes).build();
		} catch (InvalidProtocolBufferException e) {
			throw propagate(e);
		}
	};

	private final PortalServerConfig portalServerConfig;

    @Inject
    public PortalEventStoreHelperImpl(final PortalServerConfig portalServerConfig) {
        this.portalServerConfig = portalServerConfig;
    }

    @Override
    public EventStore<Message> createAndConfigureEventStore(String baseName)
            throws IOException, ClassNotFoundException {
        log.trace("PortalEventStoreHelperImpl.createAndConfigureEventStore({})", baseName);
        return configureEventStore(baseName, false);
    }

    @Override
    public EventStore<Message> loadEventStore(String baseName, final boolean readOnly) {
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

    private EventStore<Message> configureEventStore(final String baseName, boolean readOnly)
			throws IOException, ClassNotFoundException {

        Map<Class<Message>, Function<Message, byte[]>> serializers = newHashMap();
        Map<Class<Message>, Function<byte[], Message>> deserializers = newHashMap();

        serializers.put(Message.class, SERIALIZER);
		deserializers.put(Message.class, DESERIALIZER);

        String basePath = getEventStoreBasePathForBaseName(baseName);

        //noinspection unchecked
        return EventStoreFactory.create()
                .eventStoreWithBasePath(basePath)
                .withSerializers(serializers)
                .andDeserializers(deserializers)
                .inReadOnlyMode(readOnly)
                .havingMonotonicEventOrder(false)
                .build();
    }

    private String getEventStoreBasePathForBaseName(String baseName) {
        return portalServerConfig.getEventStorePath() + "/" + baseName;
    }
}
