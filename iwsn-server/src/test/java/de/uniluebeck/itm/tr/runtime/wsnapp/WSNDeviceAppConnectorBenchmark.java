package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingDecoder;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingEncoder;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.apache.log4j.Level;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.jboss.netty.handler.codec.embedder.EncoderEmbedder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

@RunWith(MockitoJUnitRunner.class)
public class WSNDeviceAppConnectorBenchmark {

	static {
		//Logging.setLoggingDefaults(Level.TRACE);
		Logging.setLoggingDefaults();
	}

	private static final WSNDeviceAppConnector.Callback NULL_CALLBACK = new WSNDeviceAppConnector.Callback() {
		@Override
		public void success(@Nullable final byte[] replyPayload) {
			// nothing to do
		}

		@Override
		public void failure(final byte responseType, final byte[] replyPayload) {
			// nothing to do
		}

		@Override
		public void timeout() {
			// nothing to do
		}
	};

	private EncoderEmbedder<ChannelBuffer> encoder;

	private DecoderEmbedder<ChannelBuffer> decoder;

	private static class MinFunction implements Function<Iterable<Float>, Float> {

		@Override
		public Float apply(@Nullable final Iterable<Float> values) {

			if (values == null) {
				throw new RuntimeException("Null values are not allowed");
			}

			float min = Float.MAX_VALUE;

			for (float value : values) {
				if (value < min) {
					min = value;
				}
			}

			return min;
		}
	}

	private static class MaxFunction implements Function<Iterable<Float>, Float> {

		@Override
		public Float apply(@Nullable final Iterable<Float> values) {

			if (values == null) {
				throw new RuntimeException("Null values are not allowed");
			}

			float max = Float.MIN_VALUE;

			for (float value : values) {
				if (value > max) {
					max = value;
				}
			}

			return max;
		}
	}

	private static class MeanFunction implements Function<Iterable<Float>, Float> {

		@Override
		public Float apply(@Nullable final Iterable<Float> values) {

			if (values == null) {
				throw new RuntimeException("Null values are not allowed");
			}

			int valueCnt = 0;
			float sum = 0f;

			for (float value : values) {
				valueCnt++;
				sum += value;
			}

			return sum / valueCnt;
		}
	}

	private static final String NODE_URN = "urn:local:0x1234";

	@Mock
	private TestbedRuntime testbedRuntime;

	@Mock
	private EventBus eventBus;

	@Mock
	private AsyncEventBus asyncEventBus;

	private WSNDeviceAppConnector connector;

	@Before
	public void setUp() throws Exception {

		final WSNDeviceAppConfiguration configuration = WSNDeviceAppConfiguration
				//.builder("urn:local:0x7856", DeviceType.ISENSE.toString())
				//.setNodeSerialInterface("/dev/tty.usbserial-001213FD")
				.builder(NODE_URN, DeviceType.MOCK.toString())
				.setNodeSerialInterface("urn:local:0x7856,10,SECONDS")
				.build();

		connector = new WSNDeviceAppConnectorImpl(configuration, eventBus, asyncEventBus);
		connector.setChannelPipeline(Lists.<Tuple<String, Multimap<String, String>>>newArrayList(), NULL_CALLBACK);
		connector.startAndWait();

		encoder = new EncoderEmbedder<ChannelBuffer>(new DleStxEtxFramingEncoder());
		decoder = new DecoderEmbedder<ChannelBuffer>(new DleStxEtxFramingDecoder());
	}

	@After
	public void tearDown() throws Exception {
		connector.stopAndWait();
	}

	@Test
	public void test() throws Exception {

		List<Float> durations = newLinkedList();

		long before, after;
		for (int i = 0; i < 100; i++) {

			final SettableFuture<Object> future = SettableFuture.create();
			final int messageNumber = i;
			final ChannelBuffer message = ChannelBuffers.buffer(5);
			message.writeByte(10 & 0xFF);
			message.writeInt(messageNumber);

			final WSNDeviceAppConnector.NodeOutputListener listener = new WSNDeviceAppConnector.NodeOutputListener() {

				@Override
				public void receivedPacket(final byte[] bytes) {

					final ChannelBuffer decodedMessage = decodeMessage(ChannelBuffers.wrappedBuffer(bytes));

					int messageTypeReceived = decodedMessage.readByte();
					int messageNumberReceived = decodedMessage.readInt();

					if (messageNumber == messageNumberReceived) {
						future.set(null);
					}
				}

				@Override
				public void receiveNotification(final String notification) {
					// don't care
				}
			};

			connector.addListener(listener);

			before = System.currentTimeMillis();
			connector.sendMessage(toByteArray(encodeMessage(message)), NULL_CALLBACK);
			future.get();
			after = System.currentTimeMillis();
			durations.add((float) (after - before));

			connector.removeListener(listener);
		}

		System.out.println("Min: " + new MinFunction().apply(durations) + " ms");
		System.out.println("Max: " + new MaxFunction().apply(durations) + " ms");
		System.out.println("Mean: " + new MeanFunction().apply(durations) + " ms");
	}

	private ChannelBuffer encodeMessage(final ChannelBuffer decodedBuffer) {
		encoder.offer(decodedBuffer);
		return encoder.poll();
	}

	private ChannelBuffer decodeMessage(final ChannelBuffer encodedBuffer) {
		decoder.offer(encodedBuffer);
		return decoder.poll();
	}

	private byte[] toByteArray(final ChannelBuffer buffer) {
		byte[] arr = new byte[buffer.readableBytes()];
		buffer.readBytes(arr);
		return arr;
	}
}
