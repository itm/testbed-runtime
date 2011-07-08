/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseListener;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;


class WSNDeviceAppImpl implements WSNDeviceApp {

	/**
	 * A callback that answers the result of an operation invocation to the invoking overlay node.
	 */
	private class ReplyingNodeApiCallback implements WSNDeviceAppConnector.Callback {

		private Messages.Msg invocationMsg;

		private ReplyingNodeApiCallback(final Messages.Msg invocationMsg) {
			this.invocationMsg = invocationMsg;
		}

		@Override
		public void success(byte[] replyPayload) {
			String message = replyPayload == null ? null : new String(replyPayload);
			sendExecutionReply(invocationMsg, 1, message);
		}

		@Override
		public void failure(byte responseType, byte[] replyPayload) {
			sendExecutionReply(invocationMsg, responseType, new String(replyPayload));
		}

		@Override
		public void timeout() {
			sendExecutionReply(invocationMsg, 0, "Communication to node timed out!");
		}

		private void sendExecutionReply(final Messages.Msg invocationMsg, final int code, final String message) {
			testbedRuntime.getUnreliableMessagingService().sendAsync(
					MessageTools.buildReply(invocationMsg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
							buildRequestStatus(code, message)
					)
			);
		}

	}

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceApp.class);

	/**
	 * The nodes URN for which this instance is created. Should only be needed for log statements. The 'rest' is done in
	 * the WSNDeviceAppConnector instance.
	 */
	private String nodeUrn;

	/**
	 * A reference to the overlay network used to receive and send messages.
	 */
	private TestbedRuntime testbedRuntime;

	/**
	 * A listener that is used to received messages from the overlay network.
	 */
	private MessageEventListener messageEventListener = new MessageEventAdapter() {

		@Override
		public void messageReceived(Messages.Msg msg) {

			boolean isRecipient = nodeUrn.equals(msg.getTo());
			boolean isOperationInvocation = WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST.equals(msg.getMsgType());
			boolean isListenerManagement = WSNApp.MSG_TYPE_LISTENER_MANAGEMENT.equals(msg.getMsgType());

			if (isRecipient && isOperationInvocation) {

				log.trace("{} => Received message of type {}...", nodeUrn, msg.getMsgType());

				WSNAppMessages.OperationInvocation invocation = parseOperation(msg);

				if (invocation != null) {
					log.trace("{} => Operation parsed: {}", nodeUrn, invocation.getOperation());
					executeOperation(invocation, msg);
				}

			} else if (isRecipient && isListenerManagement) {

				log.trace("{} => Received message of type {}...", nodeUrn, msg.getMsgType());

				try {

					WSNAppMessages.ListenerManagement management = WSNAppMessages.ListenerManagement.newBuilder()
							.mergeFrom(msg.getPayload()).build();

					executeManagement(management);

				} catch (InvalidProtocolBufferException e) {
					log.warn("InvalidProtocolBufferException while unmarshalling listener management message: " + e, e);
				}

			}
		}
	};

	/**
	 * Overlay network nodes register as a listener with this instance here to receive node outputs. The listeners node
	 * names are kept here.
	 */
	private final Set<String> nodeMessageListeners = new HashSet<String>();

	private final String nodeSerialInterface;

	private final Integer timeoutNodeAPI;

	private final String nodeUSBChipID;

	private final Integer maximumMessageRate;

	private final Integer timeoutReset;

	private final Integer timeoutFlash;

	private final String nodeType;

	private SingleRequestMultiResponseListener srmrsListener = new SingleRequestMultiResponseListener() {
		@Override
		public void receiveRequest(Messages.Msg msg, Responder responder) {

			try {

				WSNAppMessages.OperationInvocation invocation =
						WSNAppMessages.OperationInvocation.newBuilder().mergeFrom(msg.getPayload()).build();

				if (WSNAppMessages.OperationInvocation.Operation.FLASH_PROGRAMS == invocation.getOperation()) {
					executeFlashPrograms(invocation, responder);
				}

			} catch (InvalidProtocolBufferException e) {
				log.warn("{} => Error while parsing operation invocation. Ignoring...: {}", nodeUrn, e);
			}

		}
	};

	private WSNDeviceAppConnector.NodeOutputListener nodeOutputListener =
			new WSNDeviceAppConnector.NodeOutputListener() {

				@Override
				public void receivedPacket(final byte[] bytes) {

					XMLGregorianCalendar now =
							datatypeFactory
									.newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance());

					WSNAppMessages.Message.Builder messageBuilder = WSNAppMessages.Message.newBuilder()
							.setSourceNodeId(nodeUrn)
							.setTimestamp(now.toXMLFormat())
							.setBinaryData(ByteString.copyFrom(bytes));

					WSNAppMessages.Message message = messageBuilder.build();

					for (String nodeMessageListener : nodeMessageListeners) {

						if (log.isDebugEnabled()) {
							log.debug("{} => Delivering device output to overlay node {}: {}", new String[]{
									nodeUrn,
									nodeMessageListener,
									WSNAppMessageTools.toString(message, false, 200)
							}
							);
						}

						testbedRuntime.getUnreliableMessagingService().sendAsync(
								nodeUrn,
								nodeMessageListener,
								WSNApp.MSG_TYPE_LISTENER_MESSAGE,
								message.toByteArray(),
								1,
								System.currentTimeMillis() + 5000
						);
					}
				}

				@Override
				public void receiveNotification(final String notificationString) {

					WSNAppMessages.Notification message = WSNAppMessages.Notification.newBuilder()
							.setMessage(notificationString)
							.build();

					for (String nodeMessageListener : nodeMessageListeners) {

						if (log.isDebugEnabled()) {
							log.debug("{} => Delivering notification to {}: {}", new String[]{
									nodeUrn,
									nodeMessageListener,
									notificationString
							}
							);
						}

						testbedRuntime.getUnreliableMessagingService().sendAsync(
								nodeUrn,
								nodeMessageListener,
								WSNApp.MSG_TYPE_LISTENER_NOTIFICATION,
								message.toByteArray(),
								1,
								System.currentTimeMillis() + 5000
						);
					}
				}
			};

	/**
	 * The connector to the actual sensor node. May be local (i.e. attached to a serial port) or remote (i.e. (multi-hop)
	 * through remote-UART.
	 */
	private WSNDeviceAppConnector connector;

	public WSNDeviceAppImpl(final String nodeUrn, final String nodeType, final String nodeSerialInterface,
							final Integer timeoutNodeAPI, final String nodeUSBChipID, final Integer maximumMessageRate,
							final TestbedRuntime testbedRuntime,
							final Integer timeoutReset, final Integer timeoutFlash) {

		Preconditions.checkNotNull(testbedRuntime);
		Preconditions.checkNotNull(nodeUrn);
		Preconditions.checkNotNull(nodeType);

		this.testbedRuntime = testbedRuntime;
		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeSerialInterface = nodeSerialInterface;
		this.nodeUSBChipID = nodeUSBChipID;
		this.timeoutNodeAPI = timeoutNodeAPI;
		this.timeoutReset = timeoutReset;
		this.timeoutFlash = timeoutFlash;
		this.maximumMessageRate = maximumMessageRate;

		try {
			this.datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			log.error(nodeUrn + " => " + e, e);
		}

	}

	/**
	 * Parses incoming operation invocation and dispatches it to the corresponding method in the WSNDeviceAppConnector
	 * instance.
	 *
	 * @param invocation the protobuf message that describes the operation to be invoked
	 * @param msg		the protobuf message in which the operation invocation message was wrapped
	 */
	private void executeOperation(WSNAppMessages.OperationInvocation invocation, Messages.Msg msg) {

		ReplyingNodeApiCallback callback = new ReplyingNodeApiCallback(msg);

		switch (invocation.getOperation()) {

			case ARE_NODES_ALIVE:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> checkAreNodesAlive()", nodeUrn);
				connector.isNodeAlive(callback);
				break;

			case DESTROY_VIRTUAL_LINK:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> destroyVirtualLink()", nodeUrn);
				try {

					WSNAppMessages.DestroyVirtualLinkRequest destroyVirtualLinkRequest =
							WSNAppMessages.DestroyVirtualLinkRequest.parseFrom(invocation.getArguments());
					executeDestroyVirtualLink(destroyVirtualLinkRequest, callback);

				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for setVirtualLink operation: {}. Ignoring...", nodeUrn, e);
					callback.failure((byte) -1,
							"Internal server error while parsing destroyVirtualLink operation".getBytes()
					);
					return;
				}
				break;

			case DISABLE_NODE:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> disableNode()", nodeUrn);
				connector.disableNode(callback);
				break;

			case DISABLE_PHYSICAL_LINK:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> disablePhysicalLink()", nodeUrn);
				try {

					WSNAppMessages.DisablePhysicalLink disablePhysicalLink =
							WSNAppMessages.DisablePhysicalLink.parseFrom(invocation.getArguments());
					long nodeB = StringUtils.parseHexOrDecLongFromUrn(disablePhysicalLink.getNodeB());
					connector.disablePhysicalLink(nodeB, callback);

				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for disablePhysicalLink operation: {}. Ignoring...", nodeUrn,
							e
					);
					callback.failure((byte) -1,
							"Internal server error while parsing disablePhysicalLink operation".getBytes()
					);
					return;
				} catch (NumberFormatException e) {
					log.warn("{} => Couldn't parse long value for disablePhysicalLink operation: {}. Ignoring...",
							nodeUrn, e
					);
					callback.failure((byte) -1, "Destination node is not a valid long value!".getBytes());
				}
				break;

			case ENABLE_NODE:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> enableNode()", nodeUrn);
				connector.enableNode(callback);
				break;

			case ENABLE_PHYSICAL_LINK:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> enablePhysicalLink()", nodeUrn);
				try {

					WSNAppMessages.EnablePhysicalLink enablePhysicalLink =
							WSNAppMessages.EnablePhysicalLink.parseFrom(invocation.getArguments());
					long nodeB = StringUtils.parseHexOrDecLongFromUrn(enablePhysicalLink.getNodeB());
					connector.enablePhysicalLink(nodeB, callback);

				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for enablePhysicalLink operation: {}. Ignoring...", nodeUrn,
							e
					);
					callback.failure((byte) -1,
							"Internal server error while parsing enablePhysicalLink operation".getBytes()
					);
					return;
				} catch (NumberFormatException e) {
					log.warn("{} => Couldn't parse long value for enablePhysicalLink operation: {}. Ignoring...",
							nodeUrn, e
					);
					callback.failure((byte) -1, "Destination node is not a valid long value!".getBytes());
				}
				break;

			case RESET_NODES:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> resetNodes()", nodeUrn);
				connector.resetNode(callback);
				break;

			case SEND:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> send()", nodeUrn);
				try {

					WSNAppMessages.Message message = WSNAppMessages.Message.parseFrom(invocation.getArguments());
					executeSendMessage(message, callback);

				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for send operation: {}. Ignoring...", nodeUrn, e);
					callback.failure((byte) -1,
							"Internal server error while parsing send operation".getBytes()
					);
					return;
				}
				break;

			case SET_CHANNEL_PIPELINE:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> setChannelPipeline()", nodeUrn);
				try {

					WSNAppMessages.SetChannelPipelineRequest request =
							WSNAppMessages.SetChannelPipelineRequest.parseFrom(invocation.getArguments());
					executeSetChannelPipeline(request, callback);

				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for setChannelPipeline operation: {}. Ignoring...", nodeUrn,
							e
					);
					callback.failure((byte) -1,
							"Internal server error while parsing setChannelPipeline operation".getBytes()
					);
					return;
				}
				break;

			case SET_VIRTUAL_LINK:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> setVirtualLink()", nodeUrn);
				try {

					WSNAppMessages.SetVirtualLinkRequest setVirtualLinkRequest =
							WSNAppMessages.SetVirtualLinkRequest.parseFrom(invocation.getArguments());
					executeSetVirtualLink(setVirtualLinkRequest, callback);

				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for setVirtualLink operation: {}. Ignoring...", nodeUrn, e);
					callback.failure((byte) -1,
							"Internal server error while parsing setVirtualLink operation".getBytes()
					);
					return;
				}
				break;

		}

	}

	/**
	 * Executes registering and un-registering for sensor node outputs.
	 *
	 * @param management the message containing the (un)register command
	 */
	private void executeManagement(WSNAppMessages.ListenerManagement management) {
		if (WSNAppMessages.ListenerManagement.Operation.REGISTER == management.getOperation()) {
			log.debug("{} => Overlay node {} registered for device outputs", nodeUrn, management.getNodeName());
			nodeMessageListeners.add(management.getNodeName());
		} else {
			log.debug("{} => Overlay node {} unregistered from device outputs", nodeUrn, management.getNodeName());
			nodeMessageListeners.remove(management.getNodeName());
		}
	}

	private void executeSetChannelPipeline(final WSNAppMessages.SetChannelPipelineRequest request,
										   final ReplyingNodeApiCallback callback) {

		final List<Tuple<String, Multimap<String, String>>> channelHandlerConfigurations = convert(request);
		connector.setChannelPipeline(channelHandlerConfigurations, callback);
	}

	public void executeDestroyVirtualLink(final WSNAppMessages.DestroyVirtualLinkRequest destroyVirtualLinkRequest,
										  final ReplyingNodeApiCallback callback) {

		Long destinationNode;
		try {
			destinationNode = StringUtils.parseHexOrDecLongFromUrn(destroyVirtualLinkRequest.getTargetNode());
		} catch (Exception e) {
			log.warn(
					"{} => Received destinationNode URN whose suffix could not be parsed to long: {}",
					nodeUrn,
					destroyVirtualLinkRequest.getTargetNode()
			);
			callback.failure((byte) -1, "Destination node URN suffix is not a valid long value!".getBytes());
			return;
		}

		if (destinationNode != null) {
			connector.destroyVirtualLink(destinationNode, callback);
		}
	}

	public void executeSetVirtualLink(final WSNAppMessages.SetVirtualLinkRequest setVirtualLinkRequest,
									  final ReplyingNodeApiCallback callback) {

		Long destinationNode = null;
		try {
			destinationNode = StringUtils.parseHexOrDecLongFromUrn(setVirtualLinkRequest.getTargetNode());
		} catch (Exception e) {
			log.warn("{} => Received destinationNode URN whose suffix could not be parsed to long: {}",
					nodeUrn,
					setVirtualLinkRequest.getTargetNode()
			);
			callback.failure((byte) -1, "Destination node URN suffix is not a valid long value!".getBytes());
		}

		if (destinationNode != null) {
			connector.setVirtualLink(destinationNode, callback);
		}
	}

	public void executeSendMessage(final WSNAppMessages.Message message, final ReplyingNodeApiCallback callback) {

		log.debug("{} => WSNDeviceAppImpl.executeSendMessage()", nodeUrn);

		byte[] messageBytes = message.getBinaryData().toByteArray();
		connector.sendMessage(messageBytes, callback);
	}

	public void executeFlashPrograms(final WSNAppMessages.OperationInvocation invocation,
									 final SingleRequestMultiResponseListener.Responder responder) {

		try {

			WSNAppMessages.Program program = WSNAppMessages.Program.parseFrom(invocation.getArguments());
			connector.flashProgram(program, new WSNDeviceAppConnector.FlashProgramCallback() {
				@Override
				public void progress(final float percentage) {
					log.debug("{} => WSNDeviceApp.flashProgram.progress({})", nodeUrn, percentage);
					responder.sendResponse(buildRequestStatus((int) (percentage * 100), null));
				}

				@Override
				public void success(final byte[] replyPayload) {
					log.debug("{} => WSNDeviceAppImpl.flashProgram.success()", nodeUrn);
					responder.sendResponse(
							buildRequestStatus(100, replyPayload == null ? null : new String(replyPayload))
					);
				}

				@Override
				public void failure(final byte responseType, final byte[] replyPayload) {
					log.debug("{} => WSNDeviceAppImpl.failedFlashPrograms()", nodeUrn);
					responder.sendResponse(buildRequestStatus(responseType, new String(replyPayload)));
				}

				@Override
				public void timeout() {
					log.debug("{} => WSNDeviceAppImpl.timeout()", nodeUrn);
					responder.sendResponse(buildRequestStatus(-1, "Flashing node timed out"));
				}
			}
			);

		} catch (InvalidProtocolBufferException e) {
			log.warn("{} => Couldn't parse program for flash operation: {}. Ignoring...", nodeUrn, e);
		}


	}

	private WSNAppMessages.OperationInvocation parseOperation(Messages.Msg msg) {
		try {
			return WSNAppMessages.OperationInvocation.parseFrom(msg.getPayload());
		} catch (InvalidProtocolBufferException e) {
			log.warn("{} => Couldn't parse operation invocation message: {}. Ignoring...", nodeUrn, e);
			return null;
		}
	}

	private DatatypeFactory datatypeFactory = null;

	@Override
	public String getName() {
		return WSNDeviceApp.class.getSimpleName();
	}

	@Override
	public void start() throws Exception {

		log.debug("{} => WSNDeviceAppImpl.start()", nodeUrn);

		// connect to device
		connector = WSNDeviceAppConnectorFactory.create(
				nodeUrn,
				nodeType,
				nodeUSBChipID,
				nodeSerialInterface,
				timeoutNodeAPI,
				maximumMessageRate,
				testbedRuntime.getSchedulerService(),
				timeoutReset,
				timeoutFlash
		);
		connector.start();
		connector.addListener(nodeOutputListener);

		// start listening to invocation messages
		testbedRuntime.getSingleRequestMultiResponseService().addListener(
				nodeUrn,
				WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST,
				srmrsListener
		);
		testbedRuntime.getMessageEventService().addListener(messageEventListener);
	}

	@Override
	public void stop() {

		log.debug("{} => WSNDeviceAppImpl.stop()", nodeUrn);

		// first stop listening to invocation messages
		testbedRuntime.getMessageEventService().removeListener(messageEventListener);
		testbedRuntime.getSingleRequestMultiResponseService().removeListener(srmrsListener);

		// then disconnect from device
		connector.removeListener(nodeOutputListener);
		connector.stop();

	}

	/**
	 * Helper method to build a RequestStatus object for asynchronous reply to an operation invocation.
	 *
	 * @param value   the operations return code
	 * @param message a message to the invoker
	 *
	 * @return the serialized RequestStatus instance created
	 */
	private byte[] buildRequestStatus(int value, String message) {

		WSNAppMessages.RequestStatus.Status.Builder statusBuilder = WSNAppMessages.RequestStatus.Status.newBuilder()
				.setNodeId(nodeUrn)
				.setValue(value);

		if (message != null) {
			statusBuilder.setMsg(message);
		}

		WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus.newBuilder()
				.setStatus(statusBuilder)
				.build();

		return requestStatus.toByteArray();

	}

	private List<Tuple<String, Multimap<String, String>>> convert(
			final WSNAppMessages.SetChannelPipelineRequest request) {

		final List<Tuple<String, Multimap<String, String>>> result = newArrayList();

		for (WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration channelHandlerConfiguration : request
				.getChannelHandlerConfigurationsList()) {

			result.add(convert(channelHandlerConfiguration));
		}

		return result;
	}

	private Tuple<String, Multimap<String, String>> convert(
			final WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration channelHandlerConfiguration) {

		return new Tuple<String, Multimap<String, String>>(
				channelHandlerConfiguration.getName(),
				convert(channelHandlerConfiguration.getConfigurationList())
		);
	}

	private Multimap<String, String> convert(
			final List<WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.KeyValuePair> configurationList) {

		final HashMultimap<String, String> result = HashMultimap.create();
		for (WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.KeyValuePair keyValuePair : configurationList) {
			result.put(keyValuePair.getKey(), keyValuePair.getValue());
		}
		return result;
	}

}
