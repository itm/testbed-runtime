package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.eventstore.ChronicleBasedEventStore;
import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

public class PortalEventStoreHelperImpl implements PortalEventStoreHelper {

	private static final Logger log = LoggerFactory.getLogger(PortalEventStoreHelperImpl.class);

	private static final Function<ReservationEndedEvent, byte[]>
			RESERVATION_ENDED_EVENT_SERIALIZER = new Function<ReservationEndedEvent, byte[]>() {
		@Override
		public byte[] apply(ReservationEndedEvent input) {
			return input.getReservation().getSerializedKey().getBytes();
		}
	};

	private static final Function<ReservationStartedEvent, byte[]>
			RESERVATION_STARTED_EVENT_SERIALIZER = new Function<ReservationStartedEvent, byte[]>() {
		@Override
		public byte[] apply(ReservationStartedEvent input) {
			return input.getReservation().getSerializedKey().getBytes();
		}
	};

	private final ReservationManager reservationManager;

	private final PortalServerConfig portalServerConfig;

	private Function<byte[], ReservationStartedEvent> RESERVATION_STARTED_EVENT_DESERIALIZER =
			new Function<byte[], ReservationStartedEvent>() {
				@Override
				public ReservationStartedEvent apply(byte[] input) {
					try {
						String json = new String(input, "UTF-8");
						return new ReservationStartedEvent(reservationManager.getReservation(json));
					} catch (Exception e) {
						log.error("Can't deserialize RESERVATION_STARTED_EVENT", e);
						return null;
					}

				}
			};

	private Function<byte[], ReservationEndedEvent>
			RESERVATION_ENDED_EVENT_DESERIALIZER = new Function<byte[], ReservationEndedEvent>() {
				@Override
				public ReservationEndedEvent apply(byte[] input) {
					try {
						String json = new String(input, "UTF-8");
						return new ReservationEndedEvent(reservationManager.getReservation(json));
					} catch (Exception e) {
						log.error("Can't deserialize RESERVATION_ENDED_EVENT", e);
						return null;
					}

				}
			};

	private Function<byte[], DevicesAttachedEvent>
			DEVICES_ATTACHED_EVENT_DESERIALIZER = new Function<byte[], DevicesAttachedEvent>() {
				@Override
				public DevicesAttachedEvent apply(byte[] input) {
					try {
						return DevicesAttachedEvent.parseFrom(input);
					} catch (InvalidProtocolBufferException e) {
						log.error("Can't deserialize DEVICES_ATTACHED_EVENT");
						return null;
					}
				}
			};

	private Function<byte[], DevicesDetachedEvent>
			DEVICES_DETACHED_EVENT_DESERIALIZER = new Function<byte[], DevicesDetachedEvent>() {
				@Override
				public DevicesDetachedEvent apply(byte[] input) {
					try {
						return DevicesDetachedEvent.parseFrom(input);
					} catch (InvalidProtocolBufferException e) {
						log.error("Can't deserialize DEVICES_DETACHED_EVENT");
						return null;
					}
				}
			};

	private Function<byte[], UpstreamMessageEvent>
			UPSTREAM_MESSAGE_EVENT_DESERIALIZER = new Function<byte[], UpstreamMessageEvent>() {
				@Override
				public UpstreamMessageEvent apply(byte[] input) {
					try {
						return UpstreamMessageEvent.parseFrom(input);
					} catch (InvalidProtocolBufferException e) {
						log.error("Can't deserialize UPSTREAM_MESSAGE_EVENT");
						return null;
					}
				}
			};

	private Function<byte[], NotificationEvent>
			NOTIFICATION_EVENT_DESERIALIZER = new Function<byte[], NotificationEvent>() {
				@Override
				public NotificationEvent apply(byte[] input) {
					try {
						return NotificationEvent.parseFrom(input);
					} catch (InvalidProtocolBufferException e) {
						log.error("Can't deserialize NOTIFICATION_EVENT");
						return null;
					}
				}
			};

	private Function<byte[], SingleNodeResponse>
			SINGLE_NODE_RESPONSE_DESERIALIZER = new Function<byte[], SingleNodeResponse>() {
				@Override
				public SingleNodeResponse apply(byte[] input) {
					try {
						return SingleNodeResponse.parseFrom(input);
					} catch (InvalidProtocolBufferException e) {
						log.error("Can't deserialize SINGLE_NODE_RESPONSE");
						return null;
					}
				}
			};

	private Function<byte[], GetChannelPipelinesResponse>
			GET_CHANNEL_PIPELINES_RESPONSE_DESERIALIZER = new Function<byte[], GetChannelPipelinesResponse>() {
				@Override
				public GetChannelPipelinesResponse apply(byte[] input) {
					try {
						return GetChannelPipelinesResponse.parseFrom(input);
					} catch (InvalidProtocolBufferException e) {
						log.error("Can't deserialize GET_CHANNEL_PIPELINES_RESPONSE");
						return null;
					}
				}
			};

	private Function<byte[], Request> REQUEST_DESERIALIZER = new Function<byte[], Request>() {
		@Override
		public Request apply(byte[] input) {
			try {
				return Request.parseFrom(input);
			} catch (InvalidProtocolBufferException e) {
				log.error("Can't deserialize REQUEST");
				return null;
			}
		}
	};

	@Inject
	public PortalEventStoreHelperImpl(final ReservationManager reservationManager,
									  final PortalServerConfig portalServerConfig) {
		this.reservationManager = reservationManager;
		this.portalServerConfig = portalServerConfig;
	}


	@Override
	public IEventStore createAndConfigureEventStore(String serializedReservationKey) throws FileNotFoundException {
		return configureEventStore(serializedReservationKey, false);
	}

	@Override
	public IEventStore loadEventStore(String serializedReservationKey) throws InvalidParameterException {
		IEventStore store;
		try {
			store = configureEventStore(serializedReservationKey, true);
		} catch (FileNotFoundException e) {
			throw new InvalidParameterException(
					"Failed to load event store for reservation " + serializedReservationKey
			);
		}
		return store;
	}

	@Override
	public boolean eventStoreExistsForReservation(String serializedReservationKey) {
		return new File(getEventStoreBasenameForReservation(serializedReservationKey) + ".data").exists();
	}

	private IEventStore configureEventStore(final String serializedReservationKey, boolean readOnly)
			throws FileNotFoundException {

		Map<Class<?>, Function<?, byte[]>> serializers = new HashMap<Class<?>, Function<?, byte[]>>();
		Map<Class<?>, Function<byte[], ?>> deserializers = new HashMap<Class<?>, Function<byte[], ?>>();

		Function<MessageLite, byte[]> messageSerializer = new Function<MessageLite, byte[]>() {
			@Override
			public byte[] apply(MessageLite input) {
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
		serializers.put(ReservationStartedEvent.class, RESERVATION_STARTED_EVENT_SERIALIZER);
		serializers.put(ReservationEndedEvent.class, RESERVATION_ENDED_EVENT_SERIALIZER);

		deserializers.put(ReservationStartedEvent.class, RESERVATION_STARTED_EVENT_DESERIALIZER);
		deserializers.put(ReservationEndedEvent.class, RESERVATION_ENDED_EVENT_DESERIALIZER);
		deserializers.put(DevicesAttachedEvent.class, DEVICES_ATTACHED_EVENT_DESERIALIZER);
		deserializers.put(DevicesDetachedEvent.class, DEVICES_DETACHED_EVENT_DESERIALIZER);
		deserializers.put(UpstreamMessageEvent.class, UPSTREAM_MESSAGE_EVENT_DESERIALIZER);
		deserializers.put(NotificationEvent.class, NOTIFICATION_EVENT_DESERIALIZER);
		deserializers.put(SingleNodeResponse.class, SINGLE_NODE_RESPONSE_DESERIALIZER);
		deserializers.put(GetChannelPipelinesResponse.class, GET_CHANNEL_PIPELINES_RESPONSE_DESERIALIZER);
		deserializers.put(Request.class, REQUEST_DESERIALIZER);

		String baseName = getEventStoreBasenameForReservation(serializedReservationKey);
		log.trace("Creating new chronicle at {}", baseName);
		//noinspection unchecked
		return new ChronicleBasedEventStore(baseName, serializers, deserializers, readOnly);
	}


	private String getEventStoreBasenameForReservation(String serializedReservationKey) {
		return portalServerConfig.getEventStorePath() + "/" + serializedReservationKey;
	}

}
