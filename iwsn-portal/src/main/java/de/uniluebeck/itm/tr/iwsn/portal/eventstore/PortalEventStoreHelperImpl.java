package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.eventstore.ChronicleBasedEventStore;
import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
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
	public IEventStore createAndConfigureEventStore(String serializedReservationKey)
			throws FileNotFoundException, ClassNotFoundException {
		return configureEventStore(serializedReservationKey, false);
	}

	@Override
	public IEventStore loadEventStore(String serializedReservationKey, final boolean readOnly) {
		try {
			return configureEventStore(serializedReservationKey, readOnly);
		} catch (FileNotFoundException e) {
			throw new InvalidParameterException(
					"Failed to load event store for reservation " + serializedReservationKey
			);
		} catch (ClassNotFoundException e) {
			log.error("Unable to load event store.", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean eventStoreExistsForReservation(String serializedReservationKey) {
		return new File(getEventStoreBasenameForReservation(serializedReservationKey) + ".data").exists();
	}

	private IEventStore configureEventStore(final String serializedReservationKey, boolean readOnly)
			throws FileNotFoundException, ClassNotFoundException {

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

		String baseName = getEventStoreBasenameForReservation(serializedReservationKey);
		log.trace("Creating new chronicle at {}", baseName);

		//noinspection unchecked
		return new ChronicleBasedEventStore(baseName, serializers, deserializers, readOnly);
	}


	private String getEventStoreBasenameForReservation(String serializedReservationKey) {
		return portalServerConfig.getEventStorePath() + "/" + serializedReservationKey;
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
