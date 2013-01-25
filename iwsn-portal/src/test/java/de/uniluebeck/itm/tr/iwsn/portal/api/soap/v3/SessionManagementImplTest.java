package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.collect.Sets;
import de.uniluebeck.itm.nettyprotocols.HandlerFactory;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.PortalConfig;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import eu.wisebed.api.v3.common.NodeUrn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SessionManagementImplTest {

	private static final String NODE_URN_1_STRING = "urn:unit-test:0x0001";

	private static final String NODE_URN_2_STRING = "urn:unit-test:0x0002";

	private static final String NODE_URN_3_STRING = "urn:unit-test:0x0003";

	private static final NodeUrn NODE_URN_1 = new NodeUrn(NODE_URN_1_STRING);

	private static final NodeUrn NODE_URN_2 = new NodeUrn(NODE_URN_2_STRING);

	private static final NodeUrn NODE_URN_3 = new NodeUrn(NODE_URN_3_STRING);

	private static final List<NodeUrn> NODE_URNS = newArrayList(NODE_URN_1, NODE_URN_2, NODE_URN_3);

	private static final List<String> NODE_URN_STRINGS = newArrayList(NODE_URN_1_STRING, NODE_URN_2_STRING, NODE_URN_3_STRING);

	private static final String CONTROLLER_ENDPOINT_URL = "http://this.is.just.a.unit.test";

	private static final long REQUEST_ID = new Random().nextLong();

	@Mock
	private DeliveryManager deliveryManager;

	@Mock
	private PortalEventBus portalEventBus;

	@Mock
	private RequestIdProvider requestIdProvider;

	@Mock
	private ResponseTrackerFactory responseTrackerFactory;

	@Mock
	private ResponseTracker responseTracker;

	@Mock
	private PortalConfig portalConfig;

	private SessionManagementImpl sessionManagement;

	@Before
	public void setUp() throws Exception {
		sessionManagement = new SessionManagementImpl(
				deliveryManager, portalEventBus, requestIdProvider, responseTrackerFactory, portalConfig,
				Sets.<HandlerFactory>newHashSet()
		);
		when(responseTrackerFactory.create(isA(Request.class), isA(EventBusService.class))).thenReturn(responseTracker);
		when(requestIdProvider.get()).thenReturn(REQUEST_ID);
	}

	@Test
	public void testAreNodesAlive() throws Exception {

		sessionManagement.areNodesAlive(REQUEST_ID, NODE_URNS, CONTROLLER_ENDPOINT_URL);
		verify(deliveryManager).addController(CONTROLLER_ENDPOINT_URL);

		final ArgumentCaptor<Request> req1 = ArgumentCaptor.forClass(Request.class);
		verify(responseTrackerFactory).create(req1.capture(), isA(EventBusService.class));
		assertTrue(req1.getValue().getAreNodesConnectedRequest().getNodeUrnsList().equals(NODE_URN_STRINGS));

		final ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(responseTracker).addListener(runnableArgumentCaptor.capture(), Matchers.<Executor>any());

		final ArgumentCaptor<Request> req2 = ArgumentCaptor.forClass(Request.class);
		verify(portalEventBus).post(req2.capture());
		assertTrue(req2.getValue().getAreNodesConnectedRequest().getNodeUrnsList().equals(NODE_URN_STRINGS));

		runnableArgumentCaptor.getValue().run();
		verify(deliveryManager).removeController(CONTROLLER_ENDPOINT_URL);
	}
}
