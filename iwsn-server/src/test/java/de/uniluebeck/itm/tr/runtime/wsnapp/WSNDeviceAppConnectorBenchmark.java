package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryModule;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static de.uniluebeck.itm.tr.runtime.wsnapp.BenchmarkHelper.*;

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

	private static final String NODE_URN = "urn:local:0x1234";

	@Mock
	private TestbedRuntime testbedRuntime;

	@Mock
	private EventBus eventBus;

	@Mock
	private AsyncEventBus asyncEventBus;

	private WSNDeviceAppConnector connector;

	private BenchmarkHelper helper;

	@Before
	public void setUp() throws Exception {

		final WSNDeviceAppConnectorConfiguration connectorConfiguration = new WSNDeviceAppConnectorConfiguration(
				NODE_URN,
				DeviceType.ISENSE.toString(),
				"/dev/tty.usbserial-001213FD",
				null, null, null, null, null, null, null, null
		);

		/*final WSNDeviceAppConnectorConfiguration connectorConfiguration = new WSNDeviceAppConnectorConfiguration(
				NODE_URN,
				DeviceType.MOCK.toString(),
				NODE_URN + ",10,SECONDS",
				null, null, null, null, null, null, null, null
		);*/

		final Injector injector = Guice.createInjector(new WSNDeviceAppModule(), new DeviceFactoryModule());
		final WSNDeviceAppConnectorFactory factory = injector.getInstance(WSNDeviceAppConnectorFactory.class);
		final DeviceFactory deviceFactory = injector.getInstance(DeviceFactory.class);

		connector = factory.create(connectorConfiguration, deviceFactory, eventBus, asyncEventBus);
		connector.setChannelPipeline(Lists.<Tuple<String, Multimap<String, String>>>newArrayList(), NULL_CALLBACK);
		connector.startAndWait();

		helper = new BenchmarkHelper();
	}

	@After
	public void tearDown() throws Exception {
		connector.stopAndWait();
	}

	@Test
	public void testSerial() throws Exception {

		List<Float> durations = newLinkedList();

		long before, after;
		for (int i = 0; i < 1000; i++) {

			final SettableFuture<Object> future = SettableFuture.create();
			final int messageNumber = i;
			final ChannelBuffer message = ChannelBuffers.buffer(5);
			message.writeByte(10 & 0xFF);
			message.writeInt(messageNumber);

			final WSNDeviceAppConnector.NodeOutputListener listener = new WSNDeviceAppConnector.NodeOutputListener() {

				@Override
				public void receivedPacket(final byte[] bytes) {

					final ChannelBuffer decodedMessage = helper.decode(ChannelBuffers.wrappedBuffer(bytes));

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
			connector.sendMessage(toByteArray(helper.encode(message)), NULL_CALLBACK);
			future.get();
			after = System.currentTimeMillis();
			durations.add((float) (after - before));

			connector.removeListener(listener);
		}

		System.out.println("--------------------- Durations ---------------------");
		System.out.println("Min:       " + MIN.apply(durations) + " ms");
		System.out.println("Max:       " + MAX.apply(durations) + " ms");
		System.out.println("Mean:      " + MEAN.apply(durations) + " ms");
		System.out.println("Durations: " + Arrays.toString(durations.toArray()));
		System.out.println("-----------------------------------------------------");
	}

	@Test
	public void testParallel() throws Exception {
		// TODO implement parallel version
	}
}
