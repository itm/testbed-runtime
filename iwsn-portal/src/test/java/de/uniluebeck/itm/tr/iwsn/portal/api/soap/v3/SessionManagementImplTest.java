package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.collect.Sets;
import com.google.inject.Provider;
import de.uniluebeck.itm.nettyprotocols.HandlerFactory;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.sm.NodeConnectionStatus;
import eu.wisebed.wiseml.Wiseml;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.iwsn.messages.MessageFactory.newSingleNodeResponse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
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

	private static final List<String> NODE_URN_STRINGS =
			newArrayList(NODE_URN_1_STRING, NODE_URN_2_STRING, NODE_URN_3_STRING);

	private static final long REQUEST_ID = new Random().nextLong();

	@Mock
	private PortalEventBus portalEventBus;

	@Mock
	private ResponseTrackerFactory responseTrackerFactory;

	@Mock
	private ResponseTracker responseTracker;

	@Mock
	private ReservationManager reservationManager;

	@Mock
	private WSNServiceFactory wsnServiceFactory;

	@Mock
	private AuthorizingWSNFactory authorizingWSNFactory;

	@Mock
	private WSNFactory wsnFactory;

	@Mock
	private DeliveryManagerFactory deliveryManagerFactory;

	@Mock
	private DeliveryManager deliveryManager;

	@Mock
	private IdProvider requestIdProvider;

	@Mock
	private PortalServerConfig portalServerConfig;

	@Mock
	private CommonConfig commonConfig;

	@Mock
	private Provider<SessionManagementPreconditions> sessionManagementPreconditionsProvider;

	@Mock
	private Provider<Wiseml> wisemlProvider;

	@Mock
	private EndpointManager endpointManager;

	private SessionManagementImpl sessionManagement;

	@Before
	public void setUp() throws Exception {

		when(commonConfig.getUrnPrefix()).thenReturn(new NodeUrnPrefix("urn:unit-test:"));

		final Map<NodeUrn, SingleNodeResponse> responseMap = newHashMap();
		responseMap.put(NODE_URN_1, newSingleNodeResponse(null, REQUEST_ID, NODE_URN_1, 1, null));
		responseMap.put(NODE_URN_2, newSingleNodeResponse(null, REQUEST_ID, NODE_URN_2, 0, null));
		responseMap.put(NODE_URN_3, newSingleNodeResponse(null, REQUEST_ID, NODE_URN_3, 1, null));

		when(deliveryManagerFactory.create(isA(Reservation.class))).thenReturn(deliveryManager);
		when(responseTrackerFactory.create(isA(Request.class), isA(EventBusService.class))).thenReturn(responseTracker);
		when(requestIdProvider.get()).thenReturn(REQUEST_ID);
		when(responseTracker.get(anyLong(), Matchers.<TimeUnit>any())).thenReturn(responseMap);

		sessionManagement = new SessionManagementImpl(
				commonConfig,
				portalServerConfig,
				portalEventBus,
				responseTrackerFactory,
				Sets.<HandlerFactory>newHashSet(),
				reservationManager,
				wsnServiceFactory,
				authorizingWSNFactory,
				wsnFactory,
				deliveryManagerFactory,
				requestIdProvider,
				sessionManagementPreconditionsProvider,
				wisemlProvider,
				endpointManager
		);
	}

	@Test
	public void testAreNodesConnected() throws Exception {

		final List<NodeConnectionStatus> statusList = sessionManagement.areNodesConnected(NODE_URNS);

		final ArgumentCaptor<Request> req1 = ArgumentCaptor.forClass(Request.class);
		verify(responseTrackerFactory).create(req1.capture(), isA(EventBusService.class));
		assertTrue(req1.getValue().getAreNodesConnectedRequest().getNodeUrnsList().equals(NODE_URN_STRINGS));

		final ArgumentCaptor<Request> req2 = ArgumentCaptor.forClass(Request.class);
		verify(portalEventBus).post(req2.capture());
		assertTrue(req2.getValue().getAreNodesConnectedRequest().getNodeUrnsList().equals(NODE_URN_STRINGS));

		final NodeConnectionStatus node1Status = new NodeConnectionStatus();
		node1Status.setNodeUrn(NODE_URN_1);
		node1Status.setConnected(true);

		final NodeConnectionStatus node2Status = new NodeConnectionStatus();
		node2Status.setNodeUrn(NODE_URN_2);
		node2Status.setConnected(false);

		final NodeConnectionStatus node3Status = new NodeConnectionStatus();
		node3Status.setNodeUrn(NODE_URN_3);
		node3Status.setConnected(true);

		assertTrue(statusList.contains(node1Status));
		assertTrue(statusList.contains(node2Status));
		assertTrue(statusList.contains(node3Status));
	}
}
