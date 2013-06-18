package de.uniluebeck.itm.tr.iwsn.nodeapi;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.util.logging.Logging;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;


public class NodeApiImplTest {

	private static final Logger log = LoggerFactory.getLogger(NodeApiImplTest.class);

	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	private NodeApiDeviceAdapter workingDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			log.debug("NodeApiImplTest.workingDeviceAdapter.sendToNode");
			executorService.schedule(new Runnable() {
				@Override
				public void run() {

					log.debug("NodeApiImplTest.workingDeviceAdapter.run");

					if (NodeApiPackets.Interaction.isInteractionPacket(packet)) {
						workingNodeApi.receiveFromNode(NodeApiPackets.buildResponse(packet, (byte) 0, new byte[]{}));
					} else if (NodeApiPackets.LinkControl.isLinkControlPacket(packet)) {
						workingNodeApi.receiveFromNode(NodeApiPackets.buildResponse(packet, (byte) 0, new byte[]{}));
					} else if (NodeApiPackets.NetworkDescription.isNetworkDescriptionPacket(packet)) {
						workingNodeApi.receiveFromNode(NodeApiPackets.buildResponse(packet, (byte) 0, new byte[]{}));
					} else if (NodeApiPackets.NodeControl.isNodeControlPacket(packet)) {
						workingNodeApi.receiveFromNode(NodeApiPackets.buildResponse(packet, (byte) 0, new byte[]{}));
					}

				}
			}, 0, TimeUnit.MILLISECONDS
			);
		}
	};


	private NodeApiDeviceAdapter timeoutDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			log.debug("NodeApiImplTest.timeoutDeviceAdapter.sendToNode");
		}
	};

	private NodeApiImpl workingNodeApi;

	private NodeApiImpl timeoutNodeApi;

	@Before
	public void setup() {

		Logging.setLoggingDefaults();

		workingNodeApi = new NodeApiImpl("testNode", workingDeviceAdapter, 1000, TimeUnit.MILLISECONDS);
		timeoutNodeApi = new NodeApiImpl("testNode", timeoutDeviceAdapter, 5, TimeUnit.MILLISECONDS);

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

		futures.add(workingNodeApi.disablePhysicalLink(1234));
		futures.add(workingNodeApi.disablePhysicalLink(1234));
		futures.add(workingNodeApi.disablePhysicalLink(1234));
		futures.add(workingNodeApi.disablePhysicalLink(1234));

		futures.add(workingNodeApi.enablePhysicalLink(1234));
		futures.add(workingNodeApi.enablePhysicalLink(1234));
		futures.add(workingNodeApi.enablePhysicalLink(1234));
		futures.add(workingNodeApi.enablePhysicalLink(1234));

		futures.add(workingNodeApi.destroyVirtualLink(1234));
		futures.add(workingNodeApi.destroyVirtualLink(1234));
		futures.add(workingNodeApi.destroyVirtualLink(1234));
		futures.add(workingNodeApi.destroyVirtualLink(1234));

		futures.add(workingNodeApi.setVirtualLink(1234));
		futures.add(workingNodeApi.setVirtualLink(1234));
		futures.add(workingNodeApi.setVirtualLink(1234));
		futures.add(workingNodeApi.setVirtualLink(1234));

		for (Future<NodeApiCallResult> future : futures) {
			NodeApiCallResult result = future.get();
			log.debug("--------------------------------");
			Assert.assertTrue(result.isSuccessful());
		}

	}

	@Test
	public void testWorkingNodeControl() throws ExecutionException, InterruptedException {

		List<Future<NodeApiCallResult>> futures = Lists.newLinkedList();

		futures.add(workingNodeApi.enableNode());
		futures.add(workingNodeApi.enableNode());
		futures.add(workingNodeApi.enableNode());
		futures.add(workingNodeApi.enableNode());

		futures.add(workingNodeApi.disableNode());
		futures.add(workingNodeApi.disableNode());
		futures.add(workingNodeApi.disableNode());
		futures.add(workingNodeApi.disableNode());

		futures.add(workingNodeApi.areNodesAlive());
		futures.add(workingNodeApi.areNodesAlive());
		futures.add(workingNodeApi.areNodesAlive());
		futures.add(workingNodeApi.areNodesAlive());

		futures.add(workingNodeApi.resetNode(5));
		futures.add(workingNodeApi.resetNode(5));
		futures.add(workingNodeApi.resetNode(5));
		futures.add(workingNodeApi.resetNode(5));

		futures.add(workingNodeApi.setVirtualID(1));
		futures.add(workingNodeApi.setVirtualID(2));
		futures.add(workingNodeApi.setVirtualID(3));
		futures.add(workingNodeApi.setVirtualID(4));

		for (Future<NodeApiCallResult> future : futures) {
			NodeApiCallResult result = future.get();
			Assert.assertTrue(result.isSuccessful());
		}

	}

	@Test
	public void testWorkingInteraction() throws ExecutionException, InterruptedException {

		List<Future<NodeApiCallResult>> futures = Lists.newLinkedList();

		futures.add(workingNodeApi.flashProgram(new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.flashProgram(new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.flashProgram(new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.flashProgram(new byte[]{0x01, 0x02, 0x03}));

		futures.add(workingNodeApi.sendByteMessage((byte) 0x00, new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.sendByteMessage((byte) 0x00, new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.sendByteMessage((byte) 0x00, new byte[]{0x01, 0x02, 0x03}));
		futures.add(workingNodeApi.sendByteMessage((byte) 0x00, new byte[]{0x01, 0x02, 0x03}));

		futures.add(workingNodeApi.sendVirtualLinkMessage((long) 0x1234, (long) 0x2345, new byte[]{}));
		futures.add(workingNodeApi.sendVirtualLinkMessage((long) 0x1234, (long) 0x2345, new byte[]{}));
		futures.add(workingNodeApi.sendVirtualLinkMessage((long) 0x1234, (long) 0x2345, new byte[]{}));
		futures.add(workingNodeApi.sendVirtualLinkMessage((long) 0x1234, (long) 0x2345, new byte[]{}));

		for (Future<NodeApiCallResult> future : futures) {
			NodeApiCallResult result = future.get();
			Assert.assertTrue(result.isSuccessful());
		}
	}

	@Test
	public void testWorkingNetworkDescription() throws ExecutionException, InterruptedException {

		List<Future<NodeApiCallResult>> futures = Lists.newLinkedList();

		futures.add(workingNodeApi.getNeighborhood());
		futures.add(workingNodeApi.getNeighborhood());
		futures.add(workingNodeApi.getNeighborhood());
		futures.add(workingNodeApi.getNeighborhood());

		futures.add(workingNodeApi.getPropertyValue((byte) 0x00));
		futures.add(workingNodeApi.getPropertyValue((byte) 0x01));
		futures.add(workingNodeApi.getPropertyValue((byte) 0x02));
		futures.add(workingNodeApi.getPropertyValue((byte) 0x03));

		for (Future<NodeApiCallResult> future : futures) {
			NodeApiCallResult result = future.get();
			Assert.assertTrue(result.isSuccessful());
		}

	}

	@Test
	public void testTimeout() throws InterruptedException {

		testTimeoutInternal(timeoutNodeApi.disablePhysicalLink(1234));
		testTimeoutInternal(timeoutNodeApi.enablePhysicalLink(1234));
		testTimeoutInternal(timeoutNodeApi.destroyVirtualLink(1234));
		testTimeoutInternal(timeoutNodeApi.setVirtualLink(1234));

		testTimeoutInternal(timeoutNodeApi.enableNode());
		testTimeoutInternal(timeoutNodeApi.disableNode());
		testTimeoutInternal(timeoutNodeApi.areNodesAlive());
		testTimeoutInternal(timeoutNodeApi.resetNode(5));
		testTimeoutInternal(timeoutNodeApi.setVirtualID(1));

		testTimeoutInternal(timeoutNodeApi.flashProgram(new byte[]{0x01, 0x02, 0x03}));
		testTimeoutInternal(timeoutNodeApi.sendByteMessage((byte) 0x01, new byte[]{0x01, 0x02, 0x03}));
		testTimeoutInternal(timeoutNodeApi.sendVirtualLinkMessage((byte) 0x1234, (byte) 0x2345, new byte[]{}));

		testTimeoutInternal(timeoutNodeApi.getNeighborhood());
		testTimeoutInternal(timeoutNodeApi.getPropertyValue((byte) 0x00));
	}

	private void testTimeoutInternal(Future<NodeApiCallResult> future) throws InterruptedException {
		try {
			future.get();
		} catch (ExecutionException e) {
			if (!(e.getCause() instanceof TimeoutException)) {
				Assert.fail("Should have received a timeout!");
			}
		}
	}

}
