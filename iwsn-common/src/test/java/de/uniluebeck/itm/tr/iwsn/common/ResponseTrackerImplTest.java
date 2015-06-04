package de.uniluebeck.itm.tr.iwsn.common;

import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.common.IncrementalIdProvider;
import de.uniluebeck.itm.tr.common.UnixTimestampProvider;
import de.uniluebeck.itm.tr.iwsn.messages.*;
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

	private static final MessageFactory MESSAGE_FACTORY = new MessageFactoryImpl(
			new IncrementalIdProvider(),
			new UnixTimestampProvider()
	);

	private static final String NODE_URN_1_STRING = "urn:unit-test:0x0001";

	private static final String NODE_URN_2_STRING = "urn:unit-test:0x0002";

	private static final String NODE_URN_3_STRING = "urn:unit-test:0x0003";

	private static final NodeUrn NODE_URN_1 = new NodeUrn(NODE_URN_1_STRING);

	private static final NodeUrn NODE_URN_2 = new NodeUrn(NODE_URN_2_STRING);

	private static final NodeUrn NODE_URN_3 = new NodeUrn(NODE_URN_3_STRING);

	private static final long NOW = System.currentTimeMillis();

	private Header requestHeader = Header
			.newBuilder()
			.setSerializedReservationKey("" + new Random().nextLong())
			.setCorrelationId(new Random().nextLong())
			.setDownstream(true)
			.setUpstream(false)
			.setType(MessageType.REQUEST_ARE_NODES_ALIVE)
			.setTimestamp(NOW)
			.addNodeUrns(NODE_URN_1_STRING)
			.addNodeUrns(NODE_URN_2_STRING)
			.addNodeUrns(NODE_URN_3_STRING)
			.build();

	private Response node1Response = Response
			.newBuilder()
			.setHeader(Header
					.newBuilder()
					.setDownstream(true)
					.setUpstream(false)
					.setType(MessageType.REQUEST_ARE_NODES_ALIVE)
					.setTimestamp(NOW)
					.setSerializedReservationKey(requestHeader.getSerializedReservationKey())
					.setCorrelationId(requestHeader.getCorrelationId())
					.addNodeUrns(NODE_URN_1_STRING))
			.setStatusCode(1)
			.build();

	private Response node2Response = Response
			.newBuilder()
			.setHeader(Header
					.newBuilder()
					.setDownstream(true)
					.setUpstream(false)
					.setType(MessageType.REQUEST_ARE_NODES_ALIVE)
					.setTimestamp(NOW)
					.setSerializedReservationKey(requestHeader.getSerializedReservationKey())
					.setCorrelationId(requestHeader.getCorrelationId())
					.addNodeUrns(NODE_URN_2_STRING))
			.setStatusCode(1)
			.build();

	private Response node3Response = Response
			.newBuilder()
			.setHeader(Header
							.newBuilder()
							.setDownstream(true)
							.setUpstream(false)
							.setType(MessageType.REQUEST_ARE_NODES_ALIVE)
							.setTimestamp(NOW)
							.setSerializedReservationKey(requestHeader.getSerializedReservationKey())
							.setCorrelationId(requestHeader.getCorrelationId())
							.addNodeUrns(NODE_URN_3_STRING)
			)
			.setStatusCode(1)
			.build();

	private Response responseOfOtherReservationWithSameRequestId = Response
			.newBuilder()
			.setHeader(Header
					.newBuilder(node1Response.getHeader())
					.setSerializedReservationKey("" + new Random().nextLong()))
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
		responseTracker.onResponse(node1Response);
		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testIsNotDoneWhenTwoOfThreeResponsesArrived() throws Exception {

		responseTracker.onResponse(node1Response);
		responseTracker.onResponse(node2Response);

		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testIsDoneWhenAllResponsesArrived() throws Exception {

		responseTracker.onResponse(node1Response);
		responseTracker.onResponse(node2Response);
		responseTracker.onResponse(node3Response);

		assertTrue(responseTracker.isDone());
	}

	@Test
	public void testIsNotDoneWhenOneOfThreeResponsesBelongsToDifferentReservation() throws Exception {

		responseTracker.onResponse(responseOfOtherReservationWithSameRequestId);
		responseTracker.onResponse(node2Response);
		responseTracker.onResponse(node3Response);

		assertFalse(responseTracker.isDone());
	}

	@Test
	public void testResponsesAreCorrectlyMappedWhenDone() throws Exception {

		responseTracker.onResponse(node1Response);
		responseTracker.onResponse(node2Response);
		responseTracker.onResponse(node3Response);

		assertSame(node1Response, responseTracker.get().get(NODE_URN_1));
		assertSame(node2Response, responseTracker.get().get(NODE_URN_2));
		assertSame(node3Response, responseTracker.get().get(NODE_URN_3));
	}
}