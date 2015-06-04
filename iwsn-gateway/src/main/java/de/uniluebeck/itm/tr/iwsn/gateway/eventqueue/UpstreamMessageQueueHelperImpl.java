package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.inject.Inject;
import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayConfig;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper.loadOrCreateClassByteMap;

public class UpstreamMessageQueueHelperImpl implements UpstreamMessageQueueHelper {

	private static final Function<Message, byte[]> PROTOBUF_MESSAGE_SERIALIZER = Message::toByteArray;

	private final GatewayConfig gatewayConfig;

	@Inject
	public UpstreamMessageQueueHelperImpl(final GatewayConfig gatewayConfig) {
		this.gatewayConfig = gatewayConfig;
	}

	@Override
	public MultiClassSerializationHelper<Message> configureEventSerializationHelper() throws IOException, ClassNotFoundException {
		Map<Class<? extends Message>, Function<? extends Message, byte[]>> serializers = newHashMap();
		Map<Class<? extends Message>, Function<byte[], ? extends Message>> deserializers = newHashMap();

		///////////////////////// SERIALIZERS //////////////////////

		// events (upstream)

		serializers.put(Message.class, PROTOBUF_MESSAGE_SERIALIZER);


		///////////////////////// DESERIALIZERS //////////////////////

		// events (upstream)

		deserializers.put(Message.class, new Deserializer(Message.getDefaultInstance()));


		String basePath = gatewayConfig.getEventQueuePath() + "/serializers";
		File mappingFile = new File(basePath + ".mapping");

		BiMap<Class<? extends Message>, Byte> mapping = loadOrCreateClassByteMap(serializers, deserializers, mappingFile);
		return new MultiClassSerializationHelper<>(serializers, deserializers, mapping);

	}

	@Override
	public IBigQueue createAndConfigureQueue() throws IOException {
		return new BigQueueImpl(gatewayConfig.getEventQueuePath(), "event-queue");
	}

	private class Deserializer implements Function<byte[], Message> {

		private final Message defaultInstance;

		public Deserializer(final Message defaultInstance) {
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
}
