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
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.apache.log4j.Level;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.runtime.wsnapp.BenchmarkHelper.*;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

public class WSNAppBenchmark {

	private static final Logger log = LoggerFactory.getLogger(WSNAppBenchmark.class);

	static {
		Logging.setLoggingDefaults(Level.INFO);
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

	private static class FutureMessageReceiver implements WSNNodeMessageReceiver {

		private final BenchmarkHelper helper;

		private final Set<String> nodeUrns;

		private final int messageNumber;

		private final WSNAppImpl wsnApp;

		private final SettableFuture<byte[]> future;

		private final TimeDiff timeDiff = new TimeDiff();

		private long duration = Long.MAX_VALUE;

		private FutureMessageReceiver(final Set<String> nodeUrns, final int messageNumber, final WSNAppImpl wsnApp) {

			this.nodeUrns = nodeUrns;
			this.messageNumber = messageNumber;
			this.wsnApp = wsnApp;

			this.future = SettableFuture.create();
			this.helper = new BenchmarkHelper();
		}

		@Override
		public void receive(final byte[] bytes, final String sourceNodeId, final String timestamp) {

			if (log.isTraceEnabled()) {
				log.trace("Decoding received bytes: {}", StringUtils.toHexString(bytes));
			}
			final ChannelBuffer decodedMessage = helper.decode(wrappedBuffer(bytes));

			if (log.isTraceEnabled()) {
				byte[] decodedBytes = new byte[decodedMessage.readableBytes()];
				decodedMessage.getBytes(0, decodedBytes);
				log.trace("{} packet decoding complete: {}", this, StringUtils.toHexString(decodedBytes));
			}

			if (decodedMessage.getInt(1) == messageNumber) {
				log.debug("Received response for messageNumber={}", messageNumber);
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

				wsnApp.send(nodeUrns, toByteArray(helper.encode(buffer)), URN_PORTAL, "", NULL_CALLBACK);

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

		public int getMessageNumber() {
			return messageNumber;
		}
	}

	private static final String URN_NODE_0 = "urn:local:0x0000";

	private static final String URN_NODE_1 = "urn:local:0x0001";

	private static final String URN_NODE_2 = "urn:local:0x0002";

	private static final String URN_NODE_3 = "urn:local:0x0003";

	private static final String URN_NODE_4 = "urn:local:0x0004";

	private static final String URN_NODE_5 = "urn:local:0x0005";

	private static final String URN_NODE_6 = "urn:local:0x0006";

	private static final String URN_NODE_7 = "urn:local:0x0007";

	private static final String URN_NODE_8 = "urn:local:0x0008";

	private static final String URN_NODE_9 = "urn:local:0x0009";

	private static final String URN_PORTAL = "urn:local:portal";

	private static final String TCP_PORTAL = "localhost:2222";

	private static final String TCP_GATEWAY = "localhost:1111";

	private static final ImmutableSet<String> reservedNodes = ImmutableSet.of(
			URN_NODE_0,
			URN_NODE_1,
			URN_NODE_2,
			URN_NODE_3,
			URN_NODE_4,
			URN_NODE_5,
			URN_NODE_6,
			URN_NODE_7,
			URN_NODE_8,
			URN_NODE_9
	);

	private static final int RUNS = 100;

	private WSNAppImpl wsnApp;

	private TestbedRuntime gatewayTR;

	private TestbedRuntime portalTR;

	private Map<String, WSNDeviceApp> wsnDeviceApps;

	@Before
	public void setUp() throws Exception {

		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(20);

		Injector gatewayTRInjector = Guice.createInjector(
				new TestbedRuntimeModule(scheduler, scheduler, scheduler),
				new WSNDeviceAppModule()
		);

		gatewayTR = gatewayTRInjector.getInstance(TestbedRuntime.class);

		gatewayTR.getLocalNodeNameManager()
				.addLocalNodeName(URN_NODE_0)
				.addLocalNodeName(URN_NODE_1)
				.addLocalNodeName(URN_NODE_2)
				.addLocalNodeName(URN_NODE_3)
				.addLocalNodeName(URN_NODE_4)
				.addLocalNodeName(URN_NODE_5)
				.addLocalNodeName(URN_NODE_6)
				.addLocalNodeName(URN_NODE_7)
				.addLocalNodeName(URN_NODE_8)
				.addLocalNodeName(URN_NODE_9);

		gatewayTR.getRoutingTableService()
				.setNextHop(URN_PORTAL, URN_PORTAL)
				.setNextHop(URN_NODE_0, URN_NODE_0)
				.setNextHop(URN_NODE_1, URN_NODE_1)
				.setNextHop(URN_NODE_2, URN_NODE_2)
				.setNextHop(URN_NODE_3, URN_NODE_3)
				.setNextHop(URN_NODE_4, URN_NODE_4)
				.setNextHop(URN_NODE_5, URN_NODE_5)
				.setNextHop(URN_NODE_6, URN_NODE_6)
				.setNextHop(URN_NODE_7, URN_NODE_7)
				.setNextHop(URN_NODE_8, URN_NODE_8)
				.setNextHop(URN_NODE_9, URN_NODE_9);

		gatewayTR.getNamingService()
				.addEntry(new NamingEntry(URN_PORTAL, new NamingInterface("tcp", TCP_PORTAL), 1))
				.addEntry(new NamingEntry(URN_NODE_0, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_1, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_2, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_3, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_4, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_5, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_6, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_7, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_8, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_9, new NamingInterface("tcp", TCP_GATEWAY), 1));

		gatewayTR.getMessageServerService().addMessageServer("tcp", TCP_GATEWAY);

		Injector portalTRInjector = Guice.createInjector(new TestbedRuntimeModule(scheduler, scheduler, scheduler));
		portalTR = portalTRInjector.getInstance(TestbedRuntime.class);

		portalTR.getLocalNodeNameManager()
				.addLocalNodeName(URN_PORTAL);

		portalTR.getRoutingTableService()
				.setNextHop(URN_PORTAL, URN_PORTAL)
				.setNextHop(URN_NODE_0, URN_NODE_0)
				.setNextHop(URN_NODE_1, URN_NODE_1)
				.setNextHop(URN_NODE_2, URN_NODE_2)
				.setNextHop(URN_NODE_3, URN_NODE_3)
				.setNextHop(URN_NODE_4, URN_NODE_4)
				.setNextHop(URN_NODE_5, URN_NODE_5)
				.setNextHop(URN_NODE_6, URN_NODE_6)
				.setNextHop(URN_NODE_7, URN_NODE_7)
				.setNextHop(URN_NODE_8, URN_NODE_8)
				.setNextHop(URN_NODE_9, URN_NODE_9);

		portalTR.getNamingService()
				.addEntry(new NamingEntry(URN_PORTAL, new NamingInterface("tcp", TCP_PORTAL), 1))
				.addEntry(new NamingEntry(URN_NODE_0, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_1, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_2, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_3, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_4, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_5, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_6, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_7, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_8, new NamingInterface("tcp", TCP_GATEWAY), 1))
				.addEntry(new NamingEntry(URN_NODE_9, new NamingInterface("tcp", TCP_GATEWAY), 1));

		portalTR.getMessageServerService().addMessageServer("tcp", TCP_PORTAL);

		// start both nodes' stack
		gatewayTR.start();
		portalTR.start();

		createWSNDeviceApps(gatewayTRInjector);
		startWSNDeviceApps();

		wsnApp = new WSNAppImpl(portalTR, reservedNodes);
		wsnApp.startAndWait();
	}

	@After
	public void tearDown() throws Exception {

		wsnApp.stopAndWait();
		stopWSNDeviceApps();

		gatewayTR.stop();
		portalTR.stop();
	}

	@Test
	public void testSerial1Nodes() throws Exception {
		final HashSet<String> nodeUrns = newHashSet(URN_NODE_0);
		printDurations(executeSerial(nodeUrns));
	}

	@Test
	public void testSerial2Nodes() throws Exception {
		final HashSet<String> nodeUrns = newHashSet(URN_NODE_0, URN_NODE_1);
		printDurations(executeSerial(nodeUrns));
	}

	@Test
	public void testSerial3Nodes() throws Exception {
		final HashSet<String> nodeUrns = newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2);
		printDurations(executeSerial(nodeUrns));
	}

	@Test
	public void testSerial4Nodes() throws Exception {
		final HashSet<String> nodeUrns = newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3);
		printDurations(executeSerial(nodeUrns));
	}

	@Test
	public void testSerial5Nodes() throws Exception {
		final HashSet<String> nodeUrns = newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3, URN_NODE_4);
		printDurations(executeSerial(nodeUrns));
	}

	@Test
	public void testSerial1to10Nodes() throws Exception {
		printDurations(executeSerial(newHashSet(URN_NODE_0)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3, URN_NODE_4)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3, URN_NODE_4, URN_NODE_5)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3, URN_NODE_4, URN_NODE_5, URN_NODE_6)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3, URN_NODE_4, URN_NODE_5, URN_NODE_6, URN_NODE_7)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3, URN_NODE_4, URN_NODE_5, URN_NODE_6, URN_NODE_7, URN_NODE_8)));
		printDurations(executeSerial(newHashSet(URN_NODE_0, URN_NODE_1, URN_NODE_2, URN_NODE_3, URN_NODE_4, URN_NODE_5, URN_NODE_6, URN_NODE_7, URN_NODE_8, URN_NODE_9)));
	}

	@Test
	public void testParallel() throws Exception {

		final List<Float> durations = newLinkedList();
		final List<FutureMessageReceiver> receivers = newLinkedList();

		// fork
		FutureMessageReceiver receiver;
		for (int messageNumber = 0; messageNumber < RUNS; messageNumber++) {
			receiver = new FutureMessageReceiver(newHashSet(URN_NODE_1), messageNumber, wsnApp);
			receiver.start();
			receivers.add(receiver);
		}

		// join
		for (FutureMessageReceiver messageReceiver : receivers) {
			try {
				messageReceiver.getFuture().get(5, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				log.warn(
						"TimeoutException for messageNumber={} (hex: {})",
						messageReceiver.getMessageNumber(),
						StringUtils.toHexString(messageReceiver.getMessageNumber())
				);
			}
			durations.add((float) messageReceiver.getDuration());
		}

		printDurations(durations);
	}

	private void createWSNDeviceApps(final Injector injector) {
		wsnDeviceApps = new HashMap<String, WSNDeviceApp>();
		for (String nodeUrn : reservedNodes) {

			final WSNDeviceAppConfiguration configuration = new WSNDeviceAppConfiguration(nodeUrn, null);
			final WSNDeviceAppConnectorConfiguration connectorConfiguration = new WSNDeviceAppConnectorConfiguration(
					nodeUrn,
					DeviceType.MOCK.toString(),
					nodeUrn + ",10,SECONDS",
					null, null, null, null, null, null, null, null
			);

			final WSNDeviceAppGuiceFactory factory = injector.getInstance(WSNDeviceAppGuiceFactory.class);
			final WSNDeviceApp wsnDeviceApp = factory.create(gatewayTR, configuration, connectorConfiguration);
			wsnDeviceApps.put(nodeUrn, wsnDeviceApp);
		}
	}

	private void startWSNDeviceApps() {
		for (Map.Entry<String, WSNDeviceApp> entry : wsnDeviceApps.entrySet()) {
			entry.getValue().startAndWait();
		}
	}

	private void stopWSNDeviceApps() {
		for (Map.Entry<String, WSNDeviceApp> entry : wsnDeviceApps.entrySet()) {
			entry.getValue().stopAndWait();
		}
	}

	private void printDurations(final List<Float> durations) {
		System.out.println("--------------------- Durations ---------------------");
		System.out.println("Min:       " + MIN.apply(durations) + " ms");
		System.out.println("Max:       " + MAX.apply(durations) + " ms");
		System.out.println("Mean:      " + MEAN.apply(durations) + " ms");
		System.out.println("Durations: " + Arrays.toString(durations.toArray()));
		System.out.println("-----------------------------------------------------");
	}

	private List<Float> executeSerial(final HashSet<String> nodeUrns)
			throws InterruptedException, TimeoutException, ExecutionException {

		final List<Float> durations = newLinkedList();

		FutureMessageReceiver receiver;
		for (int messageNumber = 0; messageNumber < RUNS; messageNumber++) {

			receiver = new FutureMessageReceiver(nodeUrns, messageNumber, wsnApp);
			receiver.start();
			receiver.getFuture().get(5, TimeUnit.SECONDS);
			durations.add((float) receiver.getDuration());
		}

		return durations;
	}
}
