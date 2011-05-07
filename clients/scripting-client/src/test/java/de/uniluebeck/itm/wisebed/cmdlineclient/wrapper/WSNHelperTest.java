package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jws.WebParam;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;


public class WSNHelperTest {

	private WSNAsyncWrapper wrapper;

	private List<String> nodeURNs;

	private Multimap<String, String> neighborhoodMap;

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
		neighborhoodMap = HashMultimap.create();
		for (String nodeURN : nodeURNs) {
			for (String urn : nodeURNs) {
				neighborhoodMap.put(nodeURN, urn);
			}
		}
	}

	@After
	public void tearDown() {
		wrapper = null;
		nodeURNs = null;
		neighborhoodMap = null;
	}

	@Test
	public void testDisableAllPhysicalLinks() throws TimeoutException {
		assertTrue(WSNHelper.disableAllPhysicalLinks(wrapper, nodeURNs, 10000, TimeUnit.MILLISECONDS));
		try {
			WSNHelper.disableAllPhysicalLinks(wrapper, nodeURNs, 10, TimeUnit.MILLISECONDS);
			assertFalse(true);
		} catch (TimeoutException expected) {
		}
	}

	@Test
	public void testEnableAllPhysicalLinks() throws TimeoutException {
		assertTrue(WSNHelper.enableAllPhysicalLinks(wrapper, nodeURNs, 10000, TimeUnit.MILLISECONDS));
		try {
			WSNHelper.enableAllPhysicalLinks(wrapper, nodeURNs, 10, TimeUnit.MILLISECONDS);
			assertFalse(true);
		} catch (TimeoutException expected) {
		}
	}

	@Test
	public void testSetVirtualLinks() throws TimeoutException {
		assertTrue(WSNHelper.setVirtualLinks(wrapper, neighborhoodMap, "", 10000, TimeUnit.MILLISECONDS));
		try {
			WSNHelper.setVirtualLinks(wrapper, neighborhoodMap, "", 10, TimeUnit.MILLISECONDS);
			assertFalse(true);
		} catch (TimeoutException expected) {
		}
	}

	@Test
	public void testDestroyVirtualLinks() throws TimeoutException {
		assertTrue(WSNHelper.destroyVirtualLinks(wrapper, neighborhoodMap, 10000, TimeUnit.MILLISECONDS));
		try {
			WSNHelper.destroyVirtualLinks(wrapper, neighborhoodMap, 10, TimeUnit.MILLISECONDS);
			assertFalse(true);
		} catch (TimeoutException expected) {
		}
	}

}
