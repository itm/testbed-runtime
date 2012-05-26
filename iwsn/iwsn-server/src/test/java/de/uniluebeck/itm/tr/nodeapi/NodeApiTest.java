package de.uniluebeck.itm.tr.nodeapi;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.tr.util.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class NodeApiTest {

	private static final Logger log = LoggerFactory.getLogger(NodeApiTest.class);

	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	private NodeApiDeviceAdapter workingDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			log.debug("NodeApiTest.workingDeviceAdapter.sendToNode");
			executorService.schedule(new Runnable() {
				@Override
				public void run() {

					log.debug("NodeApiTest.workingDeviceAdapter.run");

					if (Packets.Interaction.isInteractionPacket(packet)) {
						workingNodeApi.receiveFromNode(Packets.buildResponse(packet, (byte) 0, new byte[]{}));
					} else if (Packets.LinkControl.isLinkControlPacket(packet)) {
						workingNodeApi.receiveFromNode(Packets.buildResponse(packet, (byte) 0, new byte[]{}));
					} else if (Packets.NetworkDescription.isNetworkDescriptionPacket(packet)) {
						workingNodeApi.receiveFromNode(Packets.buildResponse(packet, (byte) 0, new byte[]{}));
					} else if (Packets.NodeControl.isNodeControlPacket(packet)) {
						workingNodeApi.receiveFromNode(Packets.buildResponse(packet, (byte) 0, new byte[]{}));
					}

				}
			}, 0, TimeUnit.MILLISECONDS
			);
		}
	};


	private NodeApiDeviceAdapter timeoutDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			log.debug("NodeApiTest.timeoutDeviceAdapter.sendToNode");
		}
	};

	private NodeApi workingNodeApi;

	private NodeApi timeoutNodeApi;

	@Before
	public void setup() {

		Logging.setLoggingDefaults();

		workingNodeApi = new NodeApi("testNode", workingDeviceAdapter, 1000, TimeUnit.MILLISECONDS);
		timeoutNodeApi = new NodeApi("testNode", timeoutDeviceAdapter, 5, TimeUnit.MILLISECONDS);

		workingNodeApi.start();
		timeoutNodeApi.start();

	}

	@After
	public void tearDown() {

		workingNodeApi.stop();
		timeoutNodeApi.stop();

		workingNodeApi = null;
		timeoutNodeApi = null;

	}

	@Test
	public void testWorkingLinkControl() throws InterruptedException, ExecutionException {

		List<Future<NodeApiCallResult>> futures = Lists.newLinkedList();

		futures.add(workingNodeApi.getLinkControl().disablePhysicalLink(1234));
		futures.add(workingNodeApi.getLinkControl().disablePhysicalLink(1234));
		futures.add(workingNodeApi.getLinkControl().disablePhysicalLink(1234));
		futures.add(workingNodeApi.getLinkControl().disablePhysicalLink(1234));

		futures.add(workingNodeApi.getLinkControl().enablePhysicalLink(1234));
		futures.add(workingNodeApi.getLinkControl().enablePhysicalLink(1234));
		futures.add(workingNodeApi.getLinkControl().enablePhysicalLink(1234));
		futures.add(workingNodeApi.getLinkControl().enablePhysicalLink(1234));

		futures.add(workingNodeApi.getLinkControl().destroyVirtualLink(1234));
		futures.add(workingNodeApi.getLinkControl().destroyVirtualLink(1234));
		futures.add(workingNodeApi.getLinkControl().destroyVirtualLink(1234));
		futures.add(workingNodeApi.getLinkControl().destroyVirtualLink(1234));

		futures.add(workingNodeApi.getLinkControl().setVirtualLink(1234));
		futures.add(workingNodeApi.getLinkControl().setVirtualLink(1234));
		futures.add(workingNodeApi.getLinkControl().setVirtualLink(1234));
		futures.add(workingNodeApi.getLinkControl().setVirtualLink(1234));

		for (Future<NodeApiCallResult> future : futures) {
			NodeApiCallResult result = future.get();
			log.debug("--------------------------------");
			assertTrue(result.isSuccessful());
		}

	}

	@Test
	public void testWorkingNodeControl() throws ExecutionException, InterruptedException {

		List<Future<NodeApiCallResult>> futures = Lists.newLinkedList();

		futures.add(workingNodeApi.getNodeControl().enableNode());
		futures.add(workingNodeApi.getNodeControl().enableNode());
		futures.add(workingNodeApi.getNodeControl().enableNode());
		futures.add(workingNodeApi.getNodeControl().enableNode());

		futures.add(workingNodeApi.getNodeControl().disableNode());
		futures.add(workingNodeApi.getNodeControl().disableNode());
		futures.add(workingNodeApi.getNodeControl().disableNode());
		futures.add(workingNodeApi.getNodeControl().disableNode());

		futures.add(workingNodeApi.getNodeControl().areNodesAlive());
		futures.add(workingNodeApi.getNodeControl().areNodesAlive());
		futures.add(workingNodeApi.getNodeControl().areNodesAlive());
		futures.add(workingNodeApi.getNodeControl().areNodesAlive());

		futures.add(workingNodeApi.getNodeControl().resetNode(5));
		futures.add(workingNodeApi.getNodeControl().resetNode(5));
		futures.add(workingNodeApi.getNodeControl().resetNode(5));
		futures.add(workingNodeApi.getNodeControl().resetNode(5));

		futures.add(workingNodeApi.getNodeControl().setVirtualID(1));
		futures.add(workingNodeApi.getNodeControl().setVirtualID(2));
		futures.add(workingNodeApi.getNodeControl().setVirtualID(3));
		futures.add(workingNodeApi.getNodeControl().setVirtualID(4));

		for (Future<NodeApiCallResult> future : futures) {
			NodeApiCallResult result = future.get();
			assertTrue(result.isSuccessful());
		}

	}

	@Test
	public void testWorkingInteraction() throws ExecutionException, InterruptedException {

		List<Future<NodeApiCallResult>> futures = Lists.newLinkedList();

		futures.add(workingNodeApi.getInteraction().flashProgram(new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.getInteraction().flashProgram(new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.getInteraction().flashProgram(new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.getInteraction().flashProgram(new byte[]{0x01, 0x02, 0x03}));

		futures.add(workingNodeApi.getInteraction().sendByteMessage((byte) 0x00, new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.getInteraction().sendByteMessage((byte) 0x00, new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.getInteraction().sendByteMessage((byte) 0x00, new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.getInteraction().sendByteMessage((byte) 0x00, new byte[]{0x01, 0x02, 0x03}));

		futures.add(workingNodeApi.getInteraction().sendVirtualLinkMessage((long) 0x1234, (long) 0x2345, new byte[]{}));
		futures.add(workingNodeApi.getInteraction().sendVirtualLinkMessage((long) 0x1234, (long) 0x2345, new byte[]{}));
		futures.add(workingNodeApi.getInteraction().sendVirtualLinkMessage((long) 0x1234, (long) 0x2345, new byte[]{}));
		futures.add(workingNodeApi.getInteraction().sendVirtualLinkMessage((long) 0x1234, (long) 0x2345, new byte[]{}));

		for (Future<NodeApiCallResult> future : futures) {
			NodeApiCallResult result = future.get();
			assertTrue(result.isSuccessful());
		}
	}

	@Test
	public void testWorkingNetworkDescription() throws ExecutionException, InterruptedException {

		List<Future<NodeApiCallResult>> futures = Lists.newLinkedList();

		futures.add(workingNodeApi.getNetworkDescription().getNeighborhood());
		futures.add(workingNodeApi.getNetworkDescription().getNeighborhood());
		futures.add(workingNodeApi.getNetworkDescription().getNeighborhood());
		futures.add(workingNodeApi.getNetworkDescription().getNeighborhood());

		futures.add(workingNodeApi.getNetworkDescription().getPropertyValue((byte) 0x00));
		futures.add(workingNodeApi.getNetworkDescription().getPropertyValue((byte) 0x01));
		futures.add(workingNodeApi.getNetworkDescription().getPropertyValue((byte) 0x02));
		futures.add(workingNodeApi.getNetworkDescription().getPropertyValue((byte) 0x03));

		for (Future<NodeApiCallResult> future : futures) {
			NodeApiCallResult result = future.get();
			assertTrue(result.isSuccessful());
		}

	}

	@Test
	public void testTimeout() throws InterruptedException {

		testTimeoutInternal(timeoutNodeApi.getLinkControl().disablePhysicalLink(1234));
		testTimeoutInternal(timeoutNodeApi.getLinkControl().enablePhysicalLink(1234));
		testTimeoutInternal(timeoutNodeApi.getLinkControl().destroyVirtualLink(1234));
		testTimeoutInternal(timeoutNodeApi.getLinkControl().setVirtualLink(1234));

		testTimeoutInternal(timeoutNodeApi.getNodeControl().enableNode());
		testTimeoutInternal(timeoutNodeApi.getNodeControl().disableNode());
		testTimeoutInternal(timeoutNodeApi.getNodeControl().areNodesAlive());
		testTimeoutInternal(timeoutNodeApi.getNodeControl().resetNode(5));
		testTimeoutInternal(timeoutNodeApi.getNodeControl().setVirtualID(1));

		testTimeoutInternal(timeoutNodeApi.getInteraction().flashProgram(new byte[]{0x01, 0x02, 0x03}));
		testTimeoutInternal(timeoutNodeApi.getInteraction().sendByteMessage((byte) 0x01, new byte[]{0x01, 0x02, 0x03}));
		testTimeoutInternal(
				timeoutNodeApi.getInteraction().sendVirtualLinkMessage((byte) 0x1234, (byte) 0x2345, new byte[]{})
		);

		testTimeoutInternal(timeoutNodeApi.getNetworkDescription().getNeighborhood());
		testTimeoutInternal(timeoutNodeApi.getNetworkDescription().getPropertyValue((byte) 0x00));
	}

	private void testTimeoutInternal(Future<NodeApiCallResult> future) throws InterruptedException {
		try {
			future.get();
		} catch (ExecutionException e) {
			if (!(e.getCause() instanceof TimeoutException)) {
				fail("Should have received a timeout!");
			}
		}
	}

}
