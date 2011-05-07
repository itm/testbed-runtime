package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jws.WebParam;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.wsn.Program;


public class WSNAsyncWrapperTest {

	private WSNAsyncWrapper wrapper;

	private List<String> nodeURNs;

	private String sourceNodeURN;

	@Before
	public void setUp() {
		Controller controller = new Controller() {
			@Override
			public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> msg) {
				// nothing to do
			}

			@Override
			public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> status) {
				wrapper.receive(status);
			}

			@Override
			public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") final List<String> msg) {
				// nothing to do
			}

			@Override
			public void experimentEnded() {
				// nothing to do
			}
		};
		WorkingWSN wsn = new WorkingWSN(controller);
		wrapper = WSNAsyncWrapper.of(wsn);
		nodeURNs = Lists.newArrayList(
                "urn:wisebed:uzl1:0x1234",
                "urn:wisebed:uzl1:0x2345",
                "urn:wisebed:uzl1:0x3456",
                "urn:wisebed:uzl1:0x4567",
                "urn:wisebed:uzl1:0x5678",
                "urn:wisebed:uzl1:0x6789",
                "urn:wisebed:uzl1:0x7890",
                "urn:wisebed:uzl1:0x8901",
                "urn:wisebed:uzl1:0x9012"
        );
		sourceNodeURN = "urn:wisebed:uzl1:0x0123";
	}

	@After
	public void tearDown() {
		wrapper = null;
		nodeURNs = null;
		sourceNodeURN = null;
	}

	@Test
	public void testAddController() throws Exception {
		Future<Void> future = wrapper.addController("");
		future.get();
	}

	@Test
	public void testRemoveController() throws Exception {
		Future<Void> future = wrapper.removeController("");
		future.get();
	}

	@Test
	public void testSend() throws Exception {
		assertSame(100, wrapper.send(nodeURNs, new Message(), 10000, TimeUnit.MILLISECONDS).get().getSuccessPercent());
		try {
			wrapper.send(nodeURNs, new Message(), 50, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testGetVersion() throws Exception {
		assertEquals("2.3", wrapper.getVersion().get());
	}

	@Test
	public void testAreNodesAlive() throws Exception {
		assertSame(100, wrapper.areNodesAlive(nodeURNs, 10000, TimeUnit.MILLISECONDS).get().getSuccessPercent());
		try {
			wrapper.areNodesAlive(nodeURNs, 50, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testDescribeCapabilities() throws Exception {
		try {
			wrapper.describeCapabilities("");
		} catch (Exception expected) {
		}
	}

	@Test
	public void testDestroyVirtualLink() throws Exception {
		assertSame(100, wrapper.destroyVirtualLink(nodeURNs.get(0), nodeURNs.get(1), 10000, TimeUnit.MILLISECONDS)
				.get().getSuccessPercent()
		);

		try {
			wrapper.destroyVirtualLink(nodeURNs.get(0), nodeURNs.get(1), 50, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testDisableNode() throws Exception {
		assertSame(100, wrapper.disableNode(sourceNodeURN, 10000, TimeUnit.MILLISECONDS).get().getSuccessPercent());

		try {
			wrapper.disableNode(sourceNodeURN, 50, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testDisablePhysicalLink() throws Exception {
		assertSame(100, wrapper.disablePhysicalLink(nodeURNs.get(0), nodeURNs.get(1), 10000, TimeUnit.MILLISECONDS)
				.get().getSuccessPercent()
		);

		try {
			wrapper.disablePhysicalLink(nodeURNs.get(0), nodeURNs.get(1), 50, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testEnableNode() throws Exception {
		assertSame(100, wrapper.disableNode(sourceNodeURN, 10000, TimeUnit.MILLISECONDS).get().getSuccessPercent());
		try {
			wrapper.disableNode(sourceNodeURN, 50, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testEnablePhysicalLink() throws Exception {
		assertSame(100, wrapper.enablePhysicalLink(nodeURNs.get(0), nodeURNs.get(1), 10000, TimeUnit.MILLISECONDS)
				.get().getSuccessPercent()
		);
		try {
			wrapper.enablePhysicalLink(nodeURNs.get(0), nodeURNs.get(1), 50, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testFlashPrograms() throws Exception {
		assertSame(100, wrapper.flashPrograms(nodeURNs, new ArrayList<Integer>(), new ArrayList<Program>(), 10000,
				TimeUnit.MILLISECONDS
		).get().getSuccessPercent()
		);

		try {
			wrapper.flashPrograms(nodeURNs, new ArrayList<Integer>(), new ArrayList<Program>(), 50,
					TimeUnit.MILLISECONDS
			).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testGetFilters() {
		try {
			wrapper.getFilters();
		} catch (Exception expected) {
		}
	}

	@Test
	public void testGetNetwork() throws Exception {
		wrapper.getNetwork().get();
	}

	@Test
	public void testResetNodes() throws Exception {
		assertSame(100, wrapper.resetNodes(nodeURNs, 100000, TimeUnit.MILLISECONDS).get().getSuccessPercent());
		try {
			assertSame(100, wrapper.resetNodes(nodeURNs, 50, TimeUnit.MILLISECONDS).get());
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

	@Test
	public void testSetStartTime() throws Exception {

	}

	@Test
	public void testSetVirtualLink() throws Exception {
		assertSame(100,
				wrapper.setVirtualLink(nodeURNs.get(0), nodeURNs.get(1), "", null, null, 100000, TimeUnit.MILLISECONDS)
						.get().getSuccessPercent()
		);

		try {
			wrapper.setVirtualLink(nodeURNs.get(0), nodeURNs.get(1), "", null, null, 50, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException expected) {
			assertTrue(expected.getCause() instanceof TimeoutException);
		}
	}

}
