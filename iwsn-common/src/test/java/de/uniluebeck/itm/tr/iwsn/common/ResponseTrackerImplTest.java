package de.uniluebeck.itm.tr.iwsn.common;

import de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import eu.wisebed.api.v3.common.NodeUrn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Random;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ResponseTrackerImplTest {

	private static final String NODE_URN_1_STRING = "urn:unit-test:0x0001";

	private static final String NODE_URN_2_STRING = "urn:unit-test:0x0002";

	private static final String NODE_URN_3_STRING = "urn:unit-test:0x0003";

	private static final NodeUrn NODE_URN_1 = new NodeUrn(NODE_URN_1_STRING);

	private static final NodeUrn NODE_URN_2 = new NodeUrn(NODE_URN_2_STRING);

	private static final NodeUrn NODE_URN_3 = new NodeUrn(NODE_URN_3_STRING);

	private final AreNodesAliveRequest areNodesAliveRequest = AreNodesAliveRequest.newBuilder()
			.addNodeUrns(NODE_URN_1_STRING)
			.addNodeUrns(NODE_URN_2_STRING)
			.addNodeUrns(NODE_URN_3_STRING)
			.build();

	private Request request = Request.newBuilder()
			.setReservationId("" + new Random().nextLong())
			.setRequestId(new Random().nextLong())
			.setType(Request.Type.ARE_NODES_ALIVE)
			.setAreNodesAliveRequest(areNodesAliveRequest)
			.build();

	private SingleNodeResponse responseNode1 = SingleNodeResponse.newBuilder()
			.setReservationId(request.getReservationId())
			.setRequestId(request.getRequestId())
			.setNodeUrn(NODE_URN_1_STRING)
			.build();

	private SingleNodeResponse responseNode2 = SingleNodeResponse.newBuilder()
			.setReservationId(request.getReservationId())
			.setRequestId(request.getRequestId())
			.setNodeUrn(NODE_URN_2_STRING)
			.build();

	private SingleNodeResponse responseNode3 = SingleNodeResponse.newBuilder()
			.setReservationId(request.getReservationId())
			.setRequestId(request.getRequestId())
			.setNodeUrn(NODE_URN_3_STRING)
			.build();

	private SingleNodeResponse responseOfOtherReservationWithSameRequestId = SingleNodeResponse
			.newBuilder(responseNode1)
			.setReservationId("" + new Random().nextLong())
			.build();

	@Mock
	private EventBusService eventBusService;

	private ResponseTrackerImpl responseTracker;

	@Before
	public void setUp() throws Exception {
		responseTracker = new ResponseTrackerImpl(request, eventBusService);
	}

	@Test
	public void testIsNotDoneWhenNoResponsesArrived() throws Exception {
		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testIsNotDoneWhenOneOfThreeResponsesArrived() throws Exception {
		responseTracker.onSingleNodeResponse(responseNode1);
		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testIsNotDoneWhenTwoOfThreeResponsesArrived() throws Exception {

		responseTracker.onSingleNodeResponse(responseNode1);
		responseTracker.onSingleNodeResponse(responseNode2);

		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testIsDoneWhenAllResponsesArrived() throws Exception {

		responseTracker.onSingleNodeResponse(responseNode1);
		responseTracker.onSingleNodeResponse(responseNode2);
		responseTracker.onSingleNodeResponse(responseNode3);

		assertTrue(responseTracker.isDone());
	}

	@Test
	public void testIsNotDoneWhenOneOfThreeResponsesBelongsToDifferentReservation() throws Exception {

		responseTracker.onSingleNodeResponse(responseOfOtherReservationWithSameRequestId);
		responseTracker.onSingleNodeResponse(responseNode2);
		responseTracker.onSingleNodeResponse(responseNode3);

		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testResponsesAreCorrectlyMappedWhenDone() throws Exception {

		responseTracker.onSingleNodeResponse(responseNode1);
		responseTracker.onSingleNodeResponse(responseNode2);
		responseTracker.onSingleNodeResponse(responseNode3);

		assertSame(responseNode1, responseTracker.get().get(NODE_URN_1));
		assertSame(responseNode2, responseTracker.get().get(NODE_URN_2));
		assertSame(responseNode3, responseTracker.get().get(NODE_URN_3));
	}
}