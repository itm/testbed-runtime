package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.function.Function;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Sets.newHashSet;

public abstract class MessageUtils {

	private static class ProtobufDeserializer implements Function<byte[], Message> {

		private final Message defaultInstance;

		public ProtobufDeserializer(final Message defaultInstance) {
			this.defaultInstance = defaultInstance;
		}

		@Override
		public Message apply(final byte[] input) {
			try {
				return defaultInstance.newBuilderForType().mergeFrom(input).build();
			} catch (InvalidProtocolBufferException e) {
				throw propagate(e);
			}
		}
	}

	public static final Function<Message, byte[]> SERIALIZER = Message::toByteArray;

	public static final Function<byte[], Message> DESERIALIZER = new ProtobufDeserializer(Message.getDefaultInstance());

	public static boolean equals(final DevicesAttachedEvent event1, final DevicesAttachedEvent event2) {
		return event1.getHeader().getTimestamp() == event2.getHeader().getTimestamp() &&
				newHashSet(event1.getHeader().getNodeUrnsList()).equals(newHashSet(event2.getHeader().getNodeUrnsList()));
	}

	public static boolean equals(final DevicesDetachedEvent event1, final DevicesDetachedEvent event2) {
		return event1.getHeader().getTimestamp() == event2.getHeader().getTimestamp() &&
				newHashSet(event1.getHeader().getNodeUrnsList()).equals(newHashSet(event2.getHeader().getNodeUrnsList()));
	}

	public static boolean isErrorStatusCode(final Response response) {
		return response.getStatusCode() == getUnconnectedStatusCode(response.getHeader().getType()) || response.getStatusCode() < 0;
	}

	public static int getUnconnectedStatusCode(final MessageType messageType) {
		switch (messageType) {
			case REQUEST_ARE_NODES_ALIVE:
				return 0;
			case REQUEST_ARE_NODES_CONNECTED:
				return 0;
			default:
				return -1;
		}
	}
}
