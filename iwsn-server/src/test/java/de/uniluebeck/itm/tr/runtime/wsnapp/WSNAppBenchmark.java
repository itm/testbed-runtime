package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntimeModule;
import de.uniluebeck.itm.tr.iwsn.overlay.naming.NamingEntry;
import de.uniluebeck.itm.tr.iwsn.overlay.naming.NamingInterface;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.apache.log4j.Level;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.runtime.wsnapp.BenchmarkHelper.*;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

public class WSNAppBenchmark {

	private static final Logger log = LoggerFactory.getLogger(WSNAppBenchmark.class);

	static {
		Logging.setLoggingDefaults(Level.DEBUG);
	}

	private static final WSNApp.Callback NULL_CALLBACK = new WSNApp.Callback() {
		@Override
		public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
			log.debug("WSNAppBenchmark.receivedRequestStatus()");
		}

		@Override
		public void failure(final Exception e) {
			log.debug("WSNAppBenchmark.failure({})", e);
		}
	};

	private static final int RUNS = 100;

	private static class FutureMessageReceiver implements WSNNodeMessageReceiver {

		private final BenchmarkHelper helper;

		private final int messageNumber;

		private final WSNAppImpl wsnApp;

		private final SettableFuture<byte[]> future;

		private final TimeDiff timeDiff = new TimeDiff();

		private long duration = Long.MAX_VALUE;

		private FutureMessageReceiver(final BenchmarkHelper helper, final int messageNumber, final WSNAppImpl wsnApp) {
			this.future = SettableFuture.create();
			this.helper = helper;
			this.messageNumber = messageNumber;
			this.wsnApp = wsnApp;
		}

		@Override
		public void receive(final byte[] bytes, final String sourceNodeId, final String timestamp) {

			final ChannelBuffer decodedMessage = helper.decode(wrappedBuffer(bytes));

			if (decodedMessage.getInt(1) == messageNumber) {
				log.debug("Received response for messageNumber {}", messageNumber);
				duration = timeDiff.ms();
				wsnApp.removeNodeMessageReceiver(this);
				future.set(bytes);
			}
		}

		@Override
		public void receiveNotification(final WSNAppMessages.Notification notification) {
			// nothing to do
		}

		public void start() {

			final ChannelBuffer buffer = ChannelBuffers.buffer(5);
			buffer.writeByte(10 & 0xFF);
			buffer.writeInt(messageNumber);

			timeDiff.touch();
			wsnApp.addNodeMessageReceiver(this);
			try {

				wsnApp.send(
						newHashSet(URN_GATEWAY),
						toByteArray(helper.encode(buffer)),
						URN_PORTAL,
						"",
						NULL_CALLBACK
				);

			} catch (UnknownNodeUrnsException e) {
				future.setException(e);
			}
		}

		public long getDuration() {
			return duration;
		}

		public SettableFuture<byte[]> getFuture() {
			return future;
		}

		@Override
		public String toString() {
			return "FutureMessageReceiver{" +
					"messageNumber=" + messageNumber +
					'}';
		}
	}

	private static final String URN_GATEWAY = "urn:local:0x7856";

	private static final String URN_PORTAL = "urn:local:portal";

	private static final String TCP_PORTAL = "localhost:2222";

	private static final String TCP_GATEWAY = "localhost:1111";

	private WSNAppImpl wsnApp;

	private ScheduledExecutorService scheduler;

	private TestbedRuntime gateway;

	private TestbedRuntime portal;

	private WSNDeviceAppImpl wsnDeviceApp;

	private BenchmarkHelper helper;

	@Before
	public void setUp() throws Exception {

		scheduler = Executors.newScheduledThreadPool(20);

		Injector gw1Injector = Guice.createInjector(new TestbedRuntimeModule(scheduler, scheduler, scheduler));
		gateway = gw1Injector.getInstance(TestbedRuntime.class);
		gateway.getLocalNodeNameManager().addLocalNodeName(URN_GATEWAY);

		Injector gw2Injector = Guice.createInjector(new TestbedRuntimeModule(scheduler, scheduler, scheduler));
		portal = gw2Injector.getInstance(TestbedRuntime.class);
		portal.getLocalNodeNameManager().addLocalNodeName(URN_PORTAL);

		// configure topology on both nodes
		gateway.getRoutingTableService().setNextHop(URN_GATEWAY, URN_GATEWAY);
		gateway.getRoutingTableService().setNextHop(URN_PORTAL, URN_PORTAL);
		gateway.getNamingService().addEntry(new NamingEntry(URN_PORTAL, new NamingInterface("tcp", TCP_PORTAL), 1));
		gateway.getNamingService().addEntry(new NamingEntry(URN_GATEWAY, new NamingInterface("tcp", TCP_GATEWAY), 1));

		portal.getRoutingTableService().setNextHop(URN_PORTAL, URN_PORTAL);
		portal.getRoutingTableService().setNextHop(URN_GATEWAY, URN_GATEWAY);
		portal.getNamingService().addEntry(new NamingEntry(URN_PORTAL, new NamingInterface("tcp", TCP_PORTAL), 1));
		portal.getNamingService().addEntry(new NamingEntry(URN_GATEWAY, new NamingInterface("tcp", TCP_GATEWAY), 1));

		gateway.getMessageServerService().addMessageServer("tcp", TCP_GATEWAY);
		portal.getMessageServerService().addMessageServer("tcp", TCP_PORTAL);

		// start both nodes' stack
		gateway.start();
		portal.start();

		wsnApp = new WSNAppImpl(portal, ImmutableSet.of(URN_GATEWAY));

		final WSNDeviceAppConfiguration wsnDeviceAppConfiguration = WSNDeviceAppConfiguration
				.builder(URN_GATEWAY, DeviceType.MOCK.toString())
				.setNodeSerialInterface(URN_GATEWAY + ",10,SECONDS")
				.build();
		wsnDeviceApp = new WSNDeviceAppImpl(gateway, wsnDeviceAppConfiguration);

		wsnDeviceApp.startAndWait();
		wsnApp.startAndWait();

		helper = new BenchmarkHelper();
	}

	@After
	public void tearDown() throws Exception {

		wsnApp.stopAndWait();
		wsnDeviceApp.stopAndWait();

		gateway.stop();
		portal.stop();
	}

	@Test
	public void testSerial() throws Exception {

		List<Float> durations = newLinkedList();

		FutureMessageReceiver receiver;
		for (int messageNumber = 0; messageNumber < RUNS; messageNumber++) {
			receiver = new FutureMessageReceiver(helper, messageNumber, wsnApp);
			receiver.start();
			receiver.getFuture().get();
			durations.add((float) receiver.getDuration());
		}

		System.out.println("---------- Serial execution --------");
		System.out.println("Min:       " + MIN.apply(durations) + " ms");
		System.out.println("Max:       " + MAX.apply(durations) + " ms");
		System.out.println("Mean:      " + MEAN.apply(durations) + " ms");
		System.out.println("Durations: " + Arrays.toString(durations.toArray()));
	}

	@Test
	public void testParallel() throws Exception {

		final List<Float> durations = newLinkedList();
		final List<FutureMessageReceiver> receivers = newLinkedList();

		// fork
		FutureMessageReceiver receiver;
		for (int messageNumber = 0; messageNumber < RUNS; messageNumber++) {
			receiver = new FutureMessageReceiver(helper, messageNumber, wsnApp);
			receiver.start();
			receivers.add(receiver);
		}

		// join
		for (FutureMessageReceiver messageReceiver : receivers) {
			messageReceiver.getFuture().get();
			durations.add((float) messageReceiver.getDuration());
		}

		System.out.println("---------- Parallel execution --------");
		System.out.println("Min:       " + MIN.apply(durations) + " ms");
		System.out.println("Max:       " + MAX.apply(durations) + " ms");
		System.out.println("Mean:      " + MEAN.apply(durations) + " ms");
		System.out.println("Durations: " + Arrays.toString(durations.toArray()));

	}
}
