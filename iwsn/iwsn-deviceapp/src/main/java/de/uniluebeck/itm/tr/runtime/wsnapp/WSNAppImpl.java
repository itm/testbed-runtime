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
import com.google.common.collect.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingTimeoutException;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseCallback;
import de.uniluebeck.itm.gtr.messaging.unreliable.UnknownNameException;
import de.uniluebeck.itm.netty.handlerstack.FilterPipeline;
import de.uniluebeck.itm.netty.handlerstack.FilterPipelineImpl;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.netty.handlerstack.wisebed.WisebedMulticastAddress;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;


class WSNAppImpl implements WSNApp, FilterPipeline.DownstreamOutputListener, FilterPipeline.UpstreamOutputListener {

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

	private static final Logger log = LoggerFactory.getLogger(WSNApp.class);

	private static final int MSG_VALIDITY = 60000;

	private String localNodeName;

	private TestbedRuntime testbedRuntime;

	private Set<String> reservedNodes;

	private ScheduledFuture<?> registerNodeMessageReceiverFuture;

	private FilterPipeline filterPipeline;

	private HandlerFactoryRegistry handlerFactoryRegistry;

	private static final int PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE = 5000;

	private final TimeDiff pipelineMisconfigurationTimeDiff = new TimeDiff(PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE);

	private List<WSNNodeMessageReceiver> wsnNodeMessageReceivers =
			Collections.synchronizedList(new ArrayList<WSNNodeMessageReceiver>());

	private Runnable unregisterNodeMessageReceiverRunnable = new Runnable() {
		@Override
		public void run() {
			registerNodeMessageReceiver(false);
		}
	};

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
					String output = WSNAppMessageTools.toString(message, true, 200);
					output = output.endsWith("\n") ? output.substring(0, output.length() - 2) : output;
					log.debug("{}", output);
				}

				final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(message.getBinaryData().toByteArray());

				final Map<String, Object> userContext = Maps.newHashMap();
				userContext.put("timestamp", message.getTimestamp());

				final WisebedMulticastAddress sourceAddress = new WisebedMulticastAddress(
						newHashSet(message.getSourceNodeId()),
						userContext
				);
				filterPipeline.sendUpstream(buffer, sourceAddress);

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

	public WSNAppImpl(final TestbedRuntime testbedRuntime, final String[] reservedNodes) {

		this.testbedRuntime = testbedRuntime;
		this.reservedNodes = Sets.newHashSet(reservedNodes);
		this.localNodeName = testbedRuntime.getLocalNodeNames().iterator().next();

		this.filterPipeline = new FilterPipelineImpl();
		this.filterPipeline.addListener((FilterPipeline.DownstreamOutputListener) this);
		this.filterPipeline.addListener((FilterPipeline.UpstreamOutputListener) this);

		this.handlerFactoryRegistry = new HandlerFactoryRegistry();
		ProtocolCollection.registerProtocols(this.handlerFactoryRegistry);
	}

	@Override
	public void receiveDownstreamOutput(final ChannelBuffer channelBuffer, final SocketAddress socketAddress) {

		Set<String> nodeUrns;
		String timestamp;
		Callback callback;
		if (socketAddress instanceof WisebedMulticastAddress) {
			nodeUrns = ((WisebedMulticastAddress) socketAddress).getNodeUrns();
			timestamp = (String) ((WisebedMulticastAddress) socketAddress).getUserContext().get("timestamp");
			callback = (Callback) ((WisebedMulticastAddress) socketAddress).getUserContext().get("callback");
		} else {
			throw new RuntimeException(
					"Expected type " + WisebedMulticastAddress.class.getName() + "but got " + socketAddress.getClass()
							.getName() + "!"
			);
		}

		for (String nodeUrn : nodeUrns) {

			WSNAppMessages.Message message = WSNAppMessages.Message
					.newBuilder()
					.setBinaryData(ByteString.copyFrom(
							channelBuffer.array(),
							channelBuffer.readerIndex(),
							channelBuffer.readableBytes()
					)
					)
					.setSourceNodeId(nodeUrn)
					.setTimestamp(timestamp)
					.build();

			WSNAppMessages.OperationInvocation operationInvocation = WSNAppMessages.OperationInvocation
					.newBuilder()
					.setArguments(message.toByteString())
					.setOperation(WSNAppMessages.OperationInvocation.Operation.SEND)
					.build();

			try {

				// TODO use "reliable" RCP here
				testbedRuntime.getUnreliableMessagingService().sendAsync(
						localNodeName, nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST,
						operationInvocation.toByteArray(), 1,
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

				WSNAppMessages.RequestStatus.Status.Builder statusBuilder =
						WSNAppMessages.RequestStatus.Status.newBuilder()
								.setNodeId(nodeUrn)
								.setMsg("Unknown node URN \"" + nodeUrn + "\"")
								.setValue(-1);

				WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus.newBuilder()
						.setStatus(statusBuilder)
						.build();

				callback.receivedRequestStatus(requestStatus);
			}
		}

	}

	@Override
	public void downstreamExceptionCaught(final Throwable e) {
		String notificationString = "The pipeline seems to be wrongly configured. A(n) " +
				e.getCause().getClass().getSimpleName() +
				" was caught and contained the following message: " +
				e.getCause().getMessage();
		sendPipelineMisconfigurationIfNotificationRateAllows(notificationString);
	}

	private void sendPipelineMisconfigurationIfNotificationRateAllows(String notificationString) {

		if (pipelineMisconfigurationTimeDiff.isTimeout()) {

			final WSNAppMessages.Notification notification = WSNAppMessages.Notification
					.newBuilder()
					.setMessage(notificationString)
					.build();

			for (WSNNodeMessageReceiver receiver : wsnNodeMessageReceivers) {
				receiver.receiveNotification(notification);
			}

			pipelineMisconfigurationTimeDiff.touch();
		}
	}

	@Override
	public void receiveUpstreamOutput(final ChannelBuffer channelBuffer, final SocketAddress socketAddress) {

		byte[] bytes = new byte[channelBuffer.readableBytes()];
		channelBuffer.readBytes(bytes);

		String sourceNodeId = ((WisebedMulticastAddress) socketAddress).getNodeUrns().iterator().next();
		String timestamp = (String) ((WisebedMulticastAddress) socketAddress).getUserContext().get("timestamp");

		for (WSNNodeMessageReceiver receiver : wsnNodeMessageReceivers) {
			receiver.receive(bytes, sourceNodeId, timestamp);
		}
	}

	@Override
	public void upstreamExceptionCaught(final Throwable e) {
		downstreamExceptionCaught(e);
	}

	@Override
	public String getName() {
		return WSNApp.class.getSimpleName();
	}

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

		log.info("Stopping WSNApp...");

		setDefaultPipelineOnReservedNodes();
		filterPipeline.setChannelPipeline(null);

		// stop sending 'register'-messages to node counterpart
		registerNodeMessageReceiverFuture.cancel(false);

		// unregister with all nodes once
		testbedRuntime.getSchedulerService().execute(unregisterNodeMessageReceiverRunnable);

		// stop listening for messages from the nodes
		testbedRuntime.getMessageEventService().removeListener(messageEventListener);

		log.info("WSNApp stopped!");
	}

	@Override
	public void send(final Set<String> nodeUrns, final byte[] bytes, final String sourceNodeId, final String timestamp,
					 final Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(nodeUrns);

		final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(bytes);

		final HashMap<String, Object> userContext = new HashMap<String, Object>();
		userContext.put("timestamp", timestamp);
		userContext.put("callback", callback);

		final WisebedMulticastAddress targetAddress = new WisebedMulticastAddress(nodeUrns, userContext);
		filterPipeline.sendDownstream(buffer, targetAddress);
	}

	@Override
	public void setChannelPipeline(final Set<String> nodeUrns,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations,
								   final Callback callback) throws UnknownNodeUrnsException {

		boolean filterLocallyAtPortal = nodeUrns.isEmpty();

		if (filterLocallyAtPortal) {
			setChannelPipelineLocally(channelHandlerConfigurations, callback);
		} else {
			setChannelPipelineOnGateways(nodeUrns, channelHandlerConfigurations, callback);
		}
	}

	private void setChannelPipelineLocally(final List<ChannelHandlerConfiguration> channelHandlerConfigurations,
										   final Callback callback) {
		try {

			final List<Tuple<String, Multimap<String, String>>> channelHandlerConfigurations1 =
					convertCHCList(channelHandlerConfigurations);

			final List<Tuple<String,ChannelHandler>> channelPipeline =
					handlerFactoryRegistry.create(channelHandlerConfigurations1);

			if (log.isDebugEnabled() && channelHandlerConfigurations1.size() > 0) {
				channelPipeline.add(0, new Tuple<String, ChannelHandler>(
						"aboveFilterPipelineLogger",
						new AboveFilterPipelineLogger("portal")
				));
				channelPipeline.add(new Tuple<String, ChannelHandler>(
						"belowFilterPipelineLogger",
						new BelowFilterPipelineLogger("portal")
				));
			}

			filterPipeline.setChannelPipeline(channelPipeline);

			final WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus
					.newBuilder()
					.setStatus(WSNAppMessages.RequestStatus.Status.newBuilder().setValue(1).setNodeId(""))
					.build();

			callback.receivedRequestStatus(requestStatus);

		} catch (Exception e) {
			log.warn("Exception while setting channel pipeline on portal host: " + e, e);
			filterPipeline.setChannelPipeline(null);
			callback.failure(e);
		}
	}

	private void setChannelPipelineOnGateways(final Set<String> nodeUrns,
											  final List<ChannelHandlerConfiguration> channelHandlerConfigurations,
											  final Callback callback) throws UnknownNodeUrnsException {
		assertNodeUrnsKnown(nodeUrns);

		WSNAppMessages.OperationInvocation.Builder operationInvocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setArguments(convertToProtobuf(channelHandlerConfigurations).build().toByteString())
				.setOperation(WSNAppMessages.OperationInvocation.Operation.SET_CHANNEL_PIPELINE);

		final byte[] bytes = operationInvocation.build().toByteArray();

		for (String nodeUrn : nodeUrns) {

			try {
				testbedRuntime.getReliableMessagingService().sendAsync(
						localNodeName, nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
						System.currentTimeMillis() + MSG_VALIDITY,
						new RequestStatusCallback(callback, nodeUrn)
				);
			} catch (UnknownNameException e) {
				callback.failure(e);
			}
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
			log.debug("Sending checkAreNodesAlive operation invocation, bytes: {}", toPrintableString(bytes, 200));
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

	private List<Tuple<String, Multimap<String, String>>> convertCHCList(final List<ChannelHandlerConfiguration> in) {

		List<Tuple<String, Multimap<String, String>>> out = newArrayList();
		for (ChannelHandlerConfiguration configuration : in) {
			out.add(convertCHC(configuration));
		}
		return out;
	}

	private Tuple<String, Multimap<String, String>> convertCHC(final ChannelHandlerConfiguration configuration) {
		return new Tuple<String, Multimap<String, String>>(
				configuration.getName(),
				convertKeyValuePair(configuration.getConfiguration())
		);
	}

	private Multimap<String, String> convertKeyValuePair(final List<KeyValuePair> configuration) {
		Multimap<String, String> out = HashMultimap.create();
		for (KeyValuePair keyValuePair : configuration) {
			out.put(keyValuePair.getKey(), keyValuePair.getValue());
		}
		return out;
	}

	private WSNAppMessages.SetChannelPipelineRequest.Builder convertToProtobuf(
			final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {
		WSNAppMessages.SetChannelPipelineRequest.Builder argumentBuilder =
				WSNAppMessages.SetChannelPipelineRequest.newBuilder();

		for (ChannelHandlerConfiguration channelHandlerConfiguration : channelHandlerConfigurations) {
			argumentBuilder.addChannelHandlerConfigurations(convertToProtobuf(channelHandlerConfiguration));
		}
		return argumentBuilder;
	}

	private WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.Builder convertToProtobuf(
			final ChannelHandlerConfiguration channelHandlerConfiguration) {
		final WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.Builder configurationBuilder =
				WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration
						.newBuilder()
						.setName(channelHandlerConfiguration.getName());

		for (KeyValuePair keyValuePair : channelHandlerConfiguration.getConfiguration()) {
			configurationBuilder.addConfiguration(convertToProtobuf(keyValuePair));
		}
		return configurationBuilder;
	}

	private WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.KeyValuePair.Builder convertToProtobuf(
			final KeyValuePair keyValuePair) {

		return WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.KeyValuePair
				.newBuilder()
				.setKey(keyValuePair.getKey())
				.setValue(keyValuePair.getValue());
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

			String msg =
					"Ignoring request as the following node URNs are unknown: " + Joiner.on(", ").join(unknownNodeUrns);
			throw new UnknownNodeUrnsException(unknownNodeUrns, msg);
		}

	}

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

	private void setDefaultPipelineOnReservedNodes() {

		log.info("Setting ChannelPipeline to default configuration for all nodes...");

		WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.Builder chc =
				WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration
						.newBuilder()
						.setName("dlestxetx-framing");

		WSNAppMessages.SetChannelPipelineRequest.Builder scpr = WSNAppMessages.SetChannelPipelineRequest
				.newBuilder()
				.addChannelHandlerConfigurations(chc);

		WSNAppMessages.OperationInvocation.Builder operationInvocation = WSNAppMessages.OperationInvocation
				.newBuilder()
				.setArguments(scpr.build().toByteString())
				.setOperation(WSNAppMessages.OperationInvocation.Operation.SET_CHANNEL_PIPELINE);

		final byte[] bytes = operationInvocation.build().toByteArray();

		for (String nodeUrn : reservedNodes) {

			try {
				testbedRuntime.getReliableMessagingService().send(
						localNodeName, nodeUrn, MSG_TYPE_OPERATION_INVOCATION_REQUEST, bytes, 1,
						System.currentTimeMillis() + MSG_VALIDITY);
			} catch (UnknownNameException e) {
				log.error("" + e, e);
			} catch (ReliableMessagingTimeoutException e) {
				log.error("" + e, e);
			}
		}
	}
}
