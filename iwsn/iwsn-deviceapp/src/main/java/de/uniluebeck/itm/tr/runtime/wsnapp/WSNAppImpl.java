/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseCallback;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnknownNameException;
import de.uniluebeck.itm.tr.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


class WSNAppImpl implements WSNApp {

	private static final Logger log = LoggerFactory.getLogger(WSNApp.class);

	private static final int MSG_VALIDITY = 5000;

	private String localNodeName;

	private TestbedRuntime testbedRuntime;

	private Set<String> reservedNodes;

	private ScheduledFuture<?> registerNodeMessageReceiverFuture;

	public WSNAppImpl(final TestbedRuntime testbedRuntime, final String[] reservedNodes) {
		this.testbedRuntime = testbedRuntime;
		this.reservedNodes = Sets.newHashSet(reservedNodes);
		this.localNodeName = testbedRuntime.getLocalNodeNames().iterator().next();
	}

	@Override
	public String getName() {
		return WSNApp.class.getSimpleName();
	}

	private Runnable unregisterNodeMessageReceiverRunnable = new Runnable() {
		@Override
		public void run() {
			registerNodeMessageReceiver(false);
		}
	};

	private void registerNodeMessageReceiver(boolean register) {

		WSNAppMessages.ListenerManagement management = WSNAppMessages.ListenerManagement.newBuilder()
				.setNodeName(localNodeName)
				.setOperation(register ?
						WSNAppMessages.ListenerManagement.Operation.REGISTER :
						WSNAppMessages.ListenerManagement.Operation.UNREGISTER
				)
				.build();

		for (String destinationNodeName : reservedNodes) {
			testbedRuntime.getUnreliableMessagingService()
					.sendAsync(localNodeName, destinationNodeName, WSNApp.MSG_TYPE_LISTENER_MANAGEMENT,
							management.toByteArray(), 2, System.currentTimeMillis() + MSG_VALIDITY
					);
		}

	}

	private Runnable registerNodeMessageReceiverRunnable = new Runnable() {
		@Override
		public void run() {
			registerNodeMessageReceiver(true);
		}
	};

	private MessageEventListener messageEventListener = new MessageEventAdapter() {
		@Override
		public void messageReceived(Messages.Msg msg) {

			boolean fromReservedNode = reservedNodes.contains(msg.getFrom());

			if (fromReservedNode) {

				boolean isMessage = WSNApp.MSG_TYPE_LISTENER_MESSAGE.equals(msg.getMsgType());
				boolean isNotification = WSNApp.MSG_TYPE_LISTENER_NOTIFICATION.equals(msg.getMsgType());

				if (isMessage) {
					deliverMessage(msg);
				} else if (isNotification) {
					deliverNotification(msg);
				}
			}

		}

		private void deliverMessage(final Messages.Msg msg) {

			try {

				WSNAppMessages.Message message = WSNAppMessages.Message.newBuilder()
						.mergeFrom(msg.getPayload())
						.build();

				if (log.isDebugEnabled()) {
					String output = WSNAppMessageTools.toString(message, true);
					output = output.endsWith("\n") ? output.substring(0, output.length() - 2) : output;
					log.debug("{}", output);
				}

				for (WSNNodeMessageReceiver receiver : wsnNodeMessageReceivers) {
					receiver.receive(message);
				}

			} catch (InvalidProtocolBufferException e) {
				log.error("" + e, e);
			}
		}

		private void deliverNotification(final Messages.Msg msg) {

			try {

				WSNAppMessages.Notification notification = WSNAppMessages.Notification.newBuilder()
						.mergeFrom(msg.getPayload())
						.build();

				for (WSNNodeMessageReceiver receiver : wsnNodeMessageReceivers) {
					receiver.receiveNotification(notification);
				}

			} catch (InvalidProtocolBufferException e) {
				log.error("" + e, e);
			}

		}
	};


	@Override
	public void start() throws Exception {

		// start listening to sensor node output messages
		testbedRuntime.getMessageEventService().addListener(messageEventListener);

		// periodically register at the node counterpart as listener to receive output from the nodes
		registerNodeMessageReceiverFuture = testbedRuntime.getSchedulerService()
				.scheduleWithFixedDelay(registerNodeMessageReceiverRunnable, 5, 30, TimeUnit.SECONDS);

	}

	@Override
	public void stop() {

		log.debug("Stopping WSNApp...");

		// stop sending 'register'-messages to node counterpart
		registerNodeMessageReceiverFuture.cancel(false);

		// unregister with all nodes once
		testbedRuntime.getSchedulerService().execute(unregisterNodeMessageReceiverRunnable);

		// stop listening for messages from the nodes
		testbedRuntime.getMessageEventService().removeListener(messageEventListener);
	}

	private static class RequestStatusCallback implements ReliableMessagingService.AsyncCallback {

		private Callback callback;

		private String nodeUrn;

		private long instantiation;

		private RequestStatusCallback(Callback callback, String nodeUrn) {
			this.callback = callback;
			this.nodeUrn = nodeUrn;
			this.instantiation = System.currentTimeMillis();
		}

		@Override
		public void success(byte[] reply) {
			try {

				WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus
						.newBuilder()
						.mergeFrom(reply)
						.build();

				log.debug("+++ Received reply after {} milliseconds from {}.",
						(System.currentTimeMillis() - instantiation), nodeUrn
				);

				callback.receivedRequestStatus(requestStatus);

			} catch (InvalidProtocolBufferException e) {
				callbackError("Internal error occurred while delivering message...", -2);
			}
		}

		@Override
		public void failure(Exception exception) {

			log.debug("### Failed after {} milliseconds from {}.", (System.currentTimeMillis() - instantiation),
					nodeUrn
			);

			callbackError("Communication to node timed out", -1);
		}

		private void callbackError(String msg, int code) {

			WSNAppMessages.RequestStatus.Status.Builder statusBuilder = WSNAppMessages.RequestStatus.Status
					.newBuilder()
					.setNodeId(nodeUrn)
					.setMsg(msg)
					.setValue(code);

			WSNAppMessages.RequestStatus.Builder requestStatusBuilder = WSNAppMessages.RequestStatus
					.newBuilder()
					.setStatus(statusBuilder);

			log.debug("--- Received error after {} milliseconds from {}.", (System.currentTimeMillis() - instantiation),
					nodeUrn
			);

			callback.receivedRequestStatus(requestStatusBuilder.build());

		}

	}

	@Override
	public void send(final Set<String> nodeUrns, final WSNAppMessages.Message message, final Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(nodeUrns);

		WSNAppMessages.OperationInvocation.Builder builder = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setArguments(message.toByteString())
				.setOperation(WSNAppMessages.OperationInvocation.Operation.SEND);

		byte[] bytes = builder.build().toByteArray();

		for (String nodeUrn : nodeUrns) {
			try {
				testbedRuntime.getUnreliableMessagingService().sendAsync(
						localNodeName, nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
						System.currentTimeMillis() + MSG_VALIDITY
				);

				WSNAppMessages.RequestStatus.Status.Builder statusBuilder =
						WSNAppMessages.RequestStatus.Status.newBuilder()
								.setNodeId(nodeUrn)
								.setValue(1);

				WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus.newBuilder()
						.setStatus(statusBuilder)
						.build();

				callback.receivedRequestStatus(requestStatus);

			} catch (UnknownNameException e) {
				callback.failure(e);
			}
		}
	}

	private void assertNodeUrnsKnown(Collection<String> nodeUrns) throws UnknownNodeUrnsException {

		ImmutableSet<String> localNodeNames = testbedRuntime.getLocalNodeNames();
		ImmutableSet<String> remoteNodeNames = testbedRuntime.getRoutingTableService().getEntries().keySet();
		Set<String> unknownNodeUrns = null;

		for (String nodeUrn : nodeUrns) {
			if (!remoteNodeNames.contains(nodeUrn) && !localNodeNames.contains(nodeUrn)) {
				if (unknownNodeUrns == null) {
					unknownNodeUrns = new HashSet<String>();
				}
				unknownNodeUrns.add(nodeUrn);
			}
		}

		if (unknownNodeUrns != null) {

			String msg = "Ignoring request as the following node URNs are unknown: " + Joiner.on(", ").join(unknownNodeUrns);
			throw new UnknownNodeUrnsException(unknownNodeUrns, msg);
		}

	}

	@Override
	public void areNodesAlive(final Set<String> nodeUrns, final Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(nodeUrns);

		WSNAppMessages.OperationInvocation.Builder builder = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.ARE_NODES_ALIVE);

		byte[] bytes = builder.build().toByteArray();

		if (log.isDebugEnabled()) {
			log.debug("Sending checkAreNodesAlive operation invocation, bytes: {}", StringUtils.toHexString(bytes));
		}

		for (String nodeUrn : nodeUrns) {
			testbedRuntime.getReliableMessagingService().sendAsync(
					localNodeName, nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
					System.currentTimeMillis() + MSG_VALIDITY,
					new RequestStatusCallback(callback, nodeUrn)
			);
		}

	}

	@Override
	public void flashPrograms(final Map<String, WSNAppMessages.Program> programs, final Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(programs.keySet());

		WSNAppMessages.OperationInvocation operationInvocationProtobuf = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.FLASH_PROGRAMS)
				.buildPartial();

		for (Map.Entry<String, WSNAppMessages.Program> entry : programs.entrySet()) {

			final String nodeUrn = entry.getKey();

			WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
					.newBuilder(operationInvocationProtobuf)
					.setArguments(entry.getValue().toByteString())
					.build();

			Messages.Msg msg = MessageTools.buildMessage(
					localNodeName, nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, invocation.toByteArray(), 1,
					System.currentTimeMillis() + MSG_VALIDITY
			);

			SingleRequestMultiResponseCallback multiResponseCallback = new SingleRequestMultiResponseCallback() {
				@Override
				public boolean receive(byte[] response) {
					try {

						WSNAppMessages.RequestStatus requestStatus =
								WSNAppMessages.RequestStatus.newBuilder()
										.mergeFrom(ByteString.copyFrom(response)).build();

						callback.receivedRequestStatus(requestStatus);

						// cancel the job if error or complete
						return requestStatus.getStatus().getValue() < 0 || requestStatus.getStatus().getValue() >= 100;

					} catch (InvalidProtocolBufferException e) {
						log.error("Exception while parsing incoming request status: " + e, e);
					}

					return false;
				}

				@Override
				public void timeout() {

					WSNAppMessages.RequestStatus.Status.Builder statusBuilder =
							WSNAppMessages.RequestStatus.Status.newBuilder()
									.setValue(-1)
									.setMsg("Flash node operation timed out!")
									.setNodeId(nodeUrn);

					WSNAppMessages.RequestStatus requestStatus =
							WSNAppMessages.RequestStatus.newBuilder()
									.setStatus(statusBuilder)
									.build();

					callback.receivedRequestStatus(requestStatus);

				}

				@Override
				public void failure(Exception exception) {
					callback.failure(exception);
				}
			};

			testbedRuntime.getSingleRequestMultiResponseService()
					.sendUnreliableRequestUnreliableResponse(msg, 2, TimeUnit.MINUTES, multiResponseCallback);

		}
	}

	@Override
	public void resetNodes(final Set<String> nodeUrns, final Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(nodeUrns);

		WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.RESET_NODES)
				.build();

		byte[] bytes = invocation.toByteArray();

		for (String nodeUrn : nodeUrns) {
			testbedRuntime.getReliableMessagingService().sendAsync(
					localNodeName, nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
					System.currentTimeMillis() + MSG_VALIDITY,
					new RequestStatusCallback(callback, nodeUrn)
			);
		}

	}

	private List<WSNNodeMessageReceiver> wsnNodeMessageReceivers =
			Collections.synchronizedList(new ArrayList<WSNNodeMessageReceiver>());

	@Override
	public void addNodeMessageReceiver(WSNNodeMessageReceiver receiver) {
		log.debug("Adding node message receiver...");
		wsnNodeMessageReceivers.add(receiver);
	}

	@Override
	public void removeNodeMessageReceiver(WSNNodeMessageReceiver receiver) {
		while (wsnNodeMessageReceivers.remove(receiver)) {
			/* nothing to do ... */
		}
	}

	@Override
	public void setVirtualLink(String sourceNodeUrn, String targetNodeUrn, Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(sourceNodeUrn));

		WSNAppMessages.SetVirtualLinkRequest.Builder setVirtualLinkRequestBuilder = WSNAppMessages.SetVirtualLinkRequest
				.newBuilder()
				.setSourceNode(sourceNodeUrn)
				.setTargetNode(targetNodeUrn);

		WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.SET_VIRTUAL_LINK)
				.setArguments(setVirtualLinkRequestBuilder.build().toByteString())
				.build();

		byte[] bytes = invocation.toByteArray();

		testbedRuntime.getReliableMessagingService()
				.sendAsync(localNodeName, sourceNodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
						System.currentTimeMillis() + MSG_VALIDITY,
						new RequestStatusCallback(callback, sourceNodeUrn)
				);
	}

	@Override
	public void destroyVirtualLink(String sourceNodeUrn, String targetNodeUrn, Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(sourceNodeUrn));

		WSNAppMessages.DestroyVirtualLinkRequest.Builder destroyVirtualLinkRequestBuilder =
				WSNAppMessages.DestroyVirtualLinkRequest
						.newBuilder()
						.setSourceNode(sourceNodeUrn)
						.setTargetNode(targetNodeUrn);

		WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.DESTROY_VIRTUAL_LINK)
				.setArguments(destroyVirtualLinkRequestBuilder.build().toByteString())
				.build();

		byte[] bytes = invocation.toByteArray();

		testbedRuntime.getReliableMessagingService().sendAsync(localNodeName,
				sourceNodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
				System.currentTimeMillis() + MSG_VALIDITY,
				new RequestStatusCallback(callback, sourceNodeUrn)
		);

	}

	@Override
	public void disableNode(final String nodeUrn, Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(nodeUrn));

		WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.DISABLE_NODE)
				.build();

		byte[] bytes = invocation.toByteArray();

		testbedRuntime.getReliableMessagingService().sendAsync(localNodeName,
				nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
				System.currentTimeMillis() + MSG_VALIDITY,
				new RequestStatusCallback(callback, nodeUrn)
		);

	}

	@Override
	public void enableNode(final String nodeUrn, Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(nodeUrn));

		WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.ENABLE_NODE)
				.build();

		byte[] bytes = invocation.toByteArray();

		testbedRuntime.getReliableMessagingService().sendAsync(localNodeName,
				nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
				System.currentTimeMillis() + MSG_VALIDITY,
				new RequestStatusCallback(callback, nodeUrn)
		);

	}

	@Override
	public void enablePhysicalLink(final String nodeUrnA, final String nodeUrnB, Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(nodeUrnA, nodeUrnB));

		WSNAppMessages.EnablePhysicalLink enablePhysicalLink = WSNAppMessages.EnablePhysicalLink
				.newBuilder()
				.setNodeB(nodeUrnB)
				.build();

		WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.ENABLE_PHYSICAL_LINK)
				.setArguments(enablePhysicalLink.toByteString())
				.build();

		byte[] bytes = invocation.toByteArray();

		testbedRuntime.getReliableMessagingService().sendAsync(localNodeName,
				nodeUrnA, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
				System.currentTimeMillis() + MSG_VALIDITY,
				new RequestStatusCallback(callback, nodeUrnA)
		);

	}

	@Override
	public void disablePhysicalLink(final String nodeUrnA, final String nodeUrnB, Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(nodeUrnA, nodeUrnB));

		WSNAppMessages.DisablePhysicalLink disablePhysicalLink = WSNAppMessages.DisablePhysicalLink
				.newBuilder()
				.setNodeB(nodeUrnB)
				.build();

		WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.DISABLE_PHYSICAL_LINK)
				.setArguments(disablePhysicalLink.toByteString())
				.build();

		byte[] bytes = invocation.toByteArray();

		testbedRuntime.getReliableMessagingService().sendAsync(localNodeName,
				nodeUrnA, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
				System.currentTimeMillis() + MSG_VALIDITY,
				new RequestStatusCallback(callback, nodeUrnA)
		);

	}
}
