package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntimeModule;
import de.uniluebeck.itm.tr.iwsn.overlay.connection.ConnectionService;
import de.uniluebeck.itm.tr.iwsn.overlay.connection.ServerConnection;
import de.uniluebeck.itm.tr.iwsn.overlay.naming.NamingEntry;
import de.uniluebeck.itm.tr.iwsn.overlay.naming.NamingInterface;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.runtime.wsnapp.BenchmarkHelper.*;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

public class WSNAppBenchmark {

	static {
		Logging.setDebugLoggingDefaults();
	}

	private static final WSNApp.Callback NULL_CALLBACK = new WSNApp.Callback() {
		@Override
		public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
			// TODO implement
		}

		@Override
		public void failure(final Exception e) {
			// TODO implement
		}
	};

	private WSNAppImpl wsnApp;

	private ScheduledExecutorService scheduler;

	private TestbedRuntime gw1;

	private TestbedRuntime gw2;

	private ConnectionService cs1;

	private ConnectionService cs2;

	private ServerConnection sc1;

	private ServerConnection sc2;

	private WSNDeviceAppImpl wsnDeviceApp;

	private BenchmarkHelper helper;

	@Before
	public void setUp() throws Exception {

		scheduler = Executors.newScheduledThreadPool(20);

		Injector gw1Injector = Guice.createInjector(new TestbedRuntimeModule(scheduler, scheduler, scheduler));
		gw1 = gw1Injector.getInstance(TestbedRuntime.class);
		gw1.getLocalNodeNameManager().addLocalNodeName("gw1");

		Injector gw2Injector = Guice.createInjector(new TestbedRuntimeModule(scheduler, scheduler, scheduler));
		gw2 = gw2Injector.getInstance(TestbedRuntime.class);
		gw2.getLocalNodeNameManager().addLocalNodeName("gw2");

		// configure topology on both nodes
		gw1.getRoutingTableService().setNextHop("gw2", "gw2");
		gw1.getNamingService().addEntry(new NamingEntry("gw2", new NamingInterface("tcp", "localhost:2220"), 1));

		gw2.getRoutingTableService().setNextHop("gw1", "gw1");
		gw2.getNamingService().addEntry(new NamingEntry("gw1", new NamingInterface("tcp", "localhost:1110"), 1));

		// start both nodes' stack
		gw1.start();
		gw2.start();

		cs1 = gw1.getConnectionService();
		cs2 = gw2.getConnectionService();

		sc1 = cs1.getServerConnection("tcp", "localhost:1110");
		sc2 = cs2.getServerConnection("tcp", "localhost:2220");

		sc1.bind();
		sc2.bind();

		wsnApp = new WSNAppImpl(gw1, ImmutableSet.of("urn:local:0x7856"));

		final WSNDeviceAppConfiguration wsnDeviceAppConfiguration = WSNDeviceAppConfiguration
				.builder("urn:local:0x7856", DeviceType.MOCK.toString())
				.setNodeSerialInterface("urn:local:0x7856,10,SECONDS")
				.build();
		wsnDeviceApp = new WSNDeviceAppImpl(gw2, wsnDeviceAppConfiguration);

		wsnApp.start(); // TODO refactor to use Guava Service class
		wsnDeviceApp.start(); // TODO refactor to use Guava Service class

		helper = new BenchmarkHelper();
	}

	@After
	public void tearDown() throws Exception {

		wsnApp.stop();
		wsnDeviceApp.stop();

		gw1.stop();
		gw2.stop();
	}

	@Test
	public void test() throws Exception {

		List<Float> durations = newLinkedList();

		long before, after;
		for (int i = 0; i < 100; i++) {

			final int messageNumber = i;

			final ChannelBuffer buffer = ChannelBuffers.buffer(4);
			buffer.writeInt(messageNumber);

			final SettableFuture<Void> future = SettableFuture.create();

			final WSNNodeMessageReceiver receiver = new WSNNodeMessageReceiver() {
				@Override
				public void receive(final byte[] bytes, final String sourceNodeId, final String timestamp) {
					final ChannelBuffer decodedMessage = helper.decode(wrappedBuffer(bytes));
					if (decodedMessage.readInt() == messageNumber) {
						future.set(null);
					}
				}

				@Override
				public void receiveNotification(final WSNAppMessages.Notification notification) {
					// nothing to do
				}
			};

			wsnApp.addNodeMessageReceiver(receiver);

			before = System.currentTimeMillis();
			wsnApp.send(
					newHashSet("urn:local:0x7856"),
					toByteArray(helper.encode(buffer)),
					"urn:local:0x7856",
					null,
					NULL_CALLBACK
			);
			future.get();
			after = System.currentTimeMillis();

			durations.add((float) (after - before));

			wsnApp.removeNodeMessageReceiver(receiver);
		}

		System.out.println("Min: " + MIN.apply(durations) + " ms");
		System.out.println("Max: " + MAX.apply(durations) + " ms");
		System.out.println("Mean: " + MEAN.apply(durations) + " ms");
	}
}
