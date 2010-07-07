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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnknownNameException;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseCallback;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.UnknownNodeUrnException_Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


@Singleton
class WSNAppImpl implements WSNApp {

	private static final Logger log = LoggerFactory.getLogger(WSNApp.class);

	private static final int MSG_VALIDITY = 5000;

	private String localNodeName;

	private TestbedRuntime testbedRuntime;

	private ScheduledFuture<?> registerNodeMessageReceiverFuture;

	@Inject
	public WSNAppImpl(TestbedRuntime testbedRuntime) {
		this.testbedRuntime = testbedRuntime;
		this.localNodeName = testbedRuntime.getLocalNodeNames().iterator().next();
	}

	@Override
	public String getName() {
		return WSNApp.class.getSimpleName();
	}

	private Runnable registerNodeMessageReceiverRunnable = new Runnable() {
		@Override
		public void run() {

			ImmutableMap<String, String> map = testbedRuntime.getRoutingTableService().getEntries();

			WSNAppMessages.ListenerManagement management = WSNAppMessages.ListenerManagement.newBuilder()
					.setNodeName(localNodeName)
					.setOperation(WSNAppMessages.ListenerManagement.Operation.REGISTER)
					.build();

			for (String destinationNodeName : map.keySet()) {

				testbedRuntime.getUnreliableMessagingService()
						.sendAsync(localNodeName, destinationNodeName, WSNApp.MSG_TYPE_LISTENER_MANAGEMENT,
								management.toByteArray(), 2, System.currentTimeMillis() + MSG_VALIDITY
						);
			}

			// also register ourselves for all local node names
			for (String currentLocalNodeName : testbedRuntime.getLocalNodeNames()) {
				testbedRuntime.getUnreliableMessagingService()
						.sendAsync(localNodeName, currentLocalNodeName, WSNApp.MSG_TYPE_LISTENER_MANAGEMENT,
								management.toByteArray(), 2, System.currentTimeMillis() + MSG_VALIDITY
						);
			}
		}
	};

	private MessageEventListener messageEventListener = new MessageEventAdapter() {
		@Override
		public void messageReceived(Messages.Msg msg) {

			if (WSNApp.MSG_TYPE_LISTENER_MESSAGE.equals(msg.getMsgType())) {

				try {

					WSNAppMessages.Message message = WSNAppMessages.Message.newBuilder()
							.mergeFrom(msg.getPayload())
							.build();

					for (WSNNodeMessageReceiver receiver : wsnNodeMessageReceivers) {

						log.debug("Deliver node output to listener: " + message);
						receiver.receive(message);

					}

				} catch (InvalidProtocolBufferException e) {
					log.error("" + e, e);
				}

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

		// stop sending 'register'-messages to node counterpart
		registerNodeMessageReceiverFuture.cancel(false);

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
				callbackError("Internal error occured while delivering message...", -2);
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
			throws UnknownNodeUrnException_Exception {

		checkNodeUrnsKnown(nodeUrns);

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

	private void checkNodeUrnsKnown(Collection<String> nodeUrns) throws UnknownNodeUrnException_Exception {

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
			throw WSNServiceHelper.createUnknownNodeUrnException(nodeUrns);
		}

	}

	@Override
	public void areNodesAlive(final Set<String> nodeUrns, final Callback callback)
			throws UnknownNodeUrnException_Exception {

		checkNodeUrnsKnown(nodeUrns);

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
			throws UnknownNodeUrnException_Exception {

		checkNodeUrnsKnown(programs.keySet());

		WSNAppMessages.OperationInvocation operationInvocationProto = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setOperation(WSNAppMessages.OperationInvocation.Operation.FLASH_PROGRAMS)
				.buildPartial();

		for (Map.Entry<String, WSNAppMessages.Program> entry : programs.entrySet()) {

			final String nodeUrn = entry.getKey();

			WSNAppMessages.OperationInvocation invocation = WSNAppMessages.OperationInvocation
					.newBuilder(operationInvocationProto)
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
						return requestStatus.getStatus().getValue() < 0 || requestStatus.getStatus()
								.getValue() >= 100;

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
					.sendReliableRequestUnreliableResponse(msg, 30, TimeUnit.MINUTES, multiResponseCallback);

		}
	}

	@Override
	public void resetNodes(final Set<String> nodeUrns, final Callback callback)
			throws UnknownNodeUrnException_Exception {

		checkNodeUrnsKnown(nodeUrns);

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
			
		}
	}

	@Override
	public void setVirtualLink(String sourceNodeUrn, String targetNodeUrn, Callback callback)
			throws UnknownNodeUrnException_Exception {

		checkNodeUrnsKnown(Arrays.asList(sourceNodeUrn));

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
			throws UnknownNodeUrnException_Exception {

		checkNodeUrnsKnown(Arrays.asList(sourceNodeUrn));

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

}
