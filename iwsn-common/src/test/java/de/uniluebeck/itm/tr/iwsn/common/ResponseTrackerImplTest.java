package de.uniluebeck.itm.tr.iwsn.common;

import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeader;
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

	private RequestResponseHeader requestHeader = RequestResponseHeader
			.newBuilder()
			.setReservationId("" + new Random().nextLong())
			.setRequestId(new Random().nextLong())
			.addNodeUrns(NODE_URN_1_STRING)
			.addNodeUrns(NODE_URN_2_STRING)
			.addNodeUrns(NODE_URN_3_STRING)
			.build();

	private SingleNodeResponse node1Response = SingleNodeResponse
			.newBuilder()
			.setHeader(RequestResponseHeader
					.newBuilder()
					.setReservationId(requestHeader.getReservationId())
					.setRequestId(requestHeader.getRequestId())
					.addNodeUrns(NODE_URN_1_STRING))
			.setStatusCode(1)
			.build();

	private SingleNodeResponse node2Response = SingleNodeResponse
			.newBuilder()
			.setHeader(RequestResponseHeader
					.newBuilder()
					.setReservationId(requestHeader.getReservationId())
					.setRequestId(requestHeader.getRequestId())
					.addNodeUrns(NODE_URN_2_STRING))
			.setStatusCode(1)
			.build();

	private SingleNodeResponse node3Response = SingleNodeResponse
			.newBuilder()
			.setHeader(RequestResponseHeader
							.newBuilder()
							.setReservationId(requestHeader.getReservationId())
							.setRequestId(requestHeader.getRequestId())
							.addNodeUrns(NODE_URN_3_STRING)
			)
			.setStatusCode(1)
			.build();

	private SingleNodeResponse responseOfOtherReservationWithSameRequestId = SingleNodeResponse
			.newBuilder()
			.setHeader(RequestResponseHeader
					.newBuilder(node1Response.getHeader())
					.setReservationId("" + new Random().nextLong()))
			.setStatusCode(1)
			.build();

	@Mock
	private EventBusService eventBusService;

	private ResponseTrackerImpl responseTracker;

	@Before
	public void setUp() throws Exception {
		responseTracker = new ResponseTrackerImpl(requestHeader, eventBusService);
	}

	@Test
	public void testIsNotDoneWhenNoResponsesArrived() throws Exception {
		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testIsNotDoneWhenOneOfThreeResponsesArrived() throws Exception {
		responseTracker.onSingleNodeResponse(node1Response);
		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testIsNotDoneWhenTwoOfThreeResponsesArrived() throws Exception {

		responseTracker.onSingleNodeResponse(node1Response);
		responseTracker.onSingleNodeResponse(node2Response);

		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testIsDoneWhenAllResponsesArrived() throws Exception {

		responseTracker.onSingleNodeResponse(node1Response);
		responseTracker.onSingleNodeResponse(node2Response);
		responseTracker.onSingleNodeResponse(node3Response);

		assertTrue(responseTracker.isDone());
	}

	@Test
	public void testIsNotDoneWhenOneOfThreeResponsesBelongsToDifferentReservation() throws Exception {

		responseTracker.onSingleNodeResponse(responseOfOtherReservationWithSameRequestId);
		responseTracker.onSingleNodeResponse(node2Response);
		responseTracker.onSingleNodeResponse(node3Response);

		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testResponsesAreCorrectlyMappedWhenDone() throws Exception {

		responseTracker.onSingleNodeResponse(node1Response);
		responseTracker.onSingleNodeResponse(node2Response);
		responseTracker.onSingleNodeResponse(node3Response);

		assertSame(node1Response, responseTracker.get().get(NODE_URN_1));
		assertSame(node2Response, responseTracker.get().get(NODE_URN_2));
		assertSame(node3Response, responseTracker.get().get(NODE_URN_3));
	}
}