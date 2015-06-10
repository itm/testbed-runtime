package de.uniluebeck.itm.tr.iwsn.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class GatewayChannelHandlerTest {

	@Test
	public void testStartsQueueOnStart() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testStopsQueueOnStop() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testRegistersOnEventBusOnStart() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testUnregistersFromEventBusOnStop() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testQueuesEventsWhenNotConnected() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testEnqueuesGatewayConnectedEventOnConnection() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testEnqueuesDevicesAttachedEventOnConnection() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testReplaysQueueOnConnection() throws Exception {
		fail("TODO implement");
	}

	/**
	 * Tests that GatewayConnectedEvent, DevicesAttachedEvent and event replay from the queue are executed in this order.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void testOrderOfConnectedAttachedReplayOnConnection() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testFailedSendsAreEnqueuedAgain() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testThatEventsAreEnqueuedAgainAfterDisconnection() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testThatIncomingDownstreamMessagesArePostedToTheEventBus() throws Exception {
		fail("TODO implement");
	}

	@Test
	public void testThatIncomingNonDownstreamMessagesResultInRuntimeException() throws Exception {
		fail("TODO implement");
	}
}
