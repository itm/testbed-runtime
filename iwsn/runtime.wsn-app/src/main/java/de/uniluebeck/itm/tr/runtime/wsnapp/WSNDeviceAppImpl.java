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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.Nullable;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.srmr.SingleRequestMultiResponseListener;
import de.uniluebeck.itm.tr.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallback;
import de.uniluebeck.itm.tr.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicBinFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.nio.ByteBuffer;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Singleton
class WSNDeviceAppImpl implements WSNDeviceApp {

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceApp.class);

	public static final String NAME_NODE_ID = "NODE_ID";

	public static final String NAME_NODE_URN = "NODE_URN";

	private String nodeUrn;

	private long nodeId;

	private TestbedRuntime testbedRuntime;

	private MessageEventListener messageEventListener = new MessageEventAdapter() {
		@Override
		public void messageReceived(Messages.Msg msg) {

			boolean isRecipient = nodeUrn.equals(msg.getTo());
			boolean isOperationInvocation = WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST.equals(msg.getMsgType());
			boolean isListenerManagement = WSNApp.MSG_TYPE_LISTENER_MANAGEMENT.equals(msg.getMsgType());

			if (isRecipient && isOperationInvocation) {

				log.debug("Received message of type {}...", msg.getMsgType());

				WSNAppMessages.OperationInvocation invocation = parseOperation(msg);

				if (invocation != null && !isExclusiveOperationRunning()) {
					log.debug("Operation parsed: " + invocation.getOperation());
					executeOperation(invocation, msg);
				}

			} else if (isRecipient && isListenerManagement) {

				log.debug("Received message of type {}...", msg.getMsgType());

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

	private final Set<String> hackyNodeMessageListeners = new HashSet<String>();

	private NodeApiDeviceAdapter nodeApiDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			try {
				iSenseDevice.send(new MessagePacket(MESSAGE_TYPE_WISELIB_DOWNSTREAM, packet.array()));
			} catch (Exception e) {
				log.error("" + e, e);
			}
		}
	};

	private NodeApi nodeApi;

	private void executeManagement(WSNAppMessages.ListenerManagement management) {
		log.debug("WSNDeviceAppImpl.executeManagement({})", management);
		if (WSNAppMessages.ListenerManagement.Operation.REGISTER == management.getOperation()) {
			hackyNodeMessageListeners.add(management.getNodeName());
		} else {
			hackyNodeMessageListeners.remove(management.getNodeName());
		}
	}

	private Messages.Msg currentOperationInvocationMsg;

	private WSNAppMessages.OperationInvocation currentOperationInvocation;

	private TimeDiff currentOperationLastProgress;

	private iSenseDevice iSenseDevice;

	private SingleRequestMultiResponseListener.Responder currentOperationResponder;

	@Inject
	public WSNDeviceAppImpl(@Named(WSNDeviceAppImpl.NAME_NODE_URN) String nodeUrn,
							@Named(WSNDeviceAppImpl.NAME_NODE_ID) long nodeId,
							iSenseDevice iSenseDevice,
							TestbedRuntime testbedRuntime) {

		this.nodeUrn = nodeUrn;
		this.nodeId = nodeId;
		this.iSenseDevice = iSenseDevice;
		this.testbedRuntime = testbedRuntime;
		// TODO set timeout realistic
		this.nodeApi = new NodeApi(nodeApiDeviceAdapter, 100, TimeUnit.SECONDS);
	}

	private void executeOperation(WSNAppMessages.OperationInvocation invocation, Messages.Msg msg) {

		switch (invocation.getOperation()) {

			case ARE_NODES_ALIVE:
				log.debug("WSNDeviceAppImpl.executeOperation --> checkAreNodesAlive()");
				executeAreNodesAlive(invocation, msg);
				break;

			case RESET_NODES:
				log.debug("WSNDeviceAppImpl.executeOperation --> resetNodes()");
				executeResetNodes(msg, invocation);
				break;

			case SEND:
				log.debug("WSNDeviceAppImpl.executeOperation --> send()");
				try {
					WSNAppMessages.Message message = WSNAppMessages.Message.parseFrom(invocation.getArguments());
					executeSendMessage(message, msg);
				} catch (InvalidProtocolBufferException e) {
					log.warn("Couldn't parse message for send operation: {}. Ignoring...", e);
					return;
				}
				break;

			case SET_VIRTUAL_LINK:
				log.debug("WSNDeviceAppImpl.executeOperation --> setVirtualLink()");
				try {
					WSNAppMessages.SetVirtualLinkRequest setVirtualLinkRequest =
							WSNAppMessages.SetVirtualLinkRequest.parseFrom(invocation.getArguments());
					executeSetVirtualLink(setVirtualLinkRequest, msg);
				} catch (InvalidProtocolBufferException e) {
					log.warn("Couldn't parse message for setVirtualLink operation: {}. Ignoring...", e);
					return;
				}
				break;

			case DESTROY_VIRTUAL_LINK:
				log.debug("WSNDeviceAppImpl.executeOperation --> destroyVirtualLink()");
				try {
					WSNAppMessages.DestroyVirtualLinkRequest destroyVirtualLinkRequest =
							WSNAppMessages.DestroyVirtualLinkRequest.parseFrom(invocation.getArguments());
					executeDestroyVirtualLink(destroyVirtualLinkRequest, msg);
				} catch (InvalidProtocolBufferException e) {
					log.warn("Couldn't parse message for setVirtualLink operation: {}. Ignoring...", e);
					return;
				}
				break;

		}

	}

	private void executeDestroyVirtualLink(final WSNAppMessages.DestroyVirtualLinkRequest destroyVirtualLinkRequest,
										   final Messages.Msg msg) {

		Long destinationNode = null;
		try {

			String[] strings = destroyVirtualLinkRequest.getTargetNode().split(":");
			destinationNode = Long.parseLong(strings[strings.length - 1]);

		} catch (Exception e) {

			log.warn("Received destinationNode URN whose suffix could not be parsed to long: {}",
					destroyVirtualLinkRequest.getTargetNode()
			);
			testbedRuntime.getUnreliableMessagingService()
					.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
							buildRequestStatus(-1, "Destination node URN suffix is not a valid long value!")
					)
					);

		}

		if (destinationNode != null) {

			nodeApi.getLinkControl().destroyVirtualLink(destinationNode, new NodeApiCallback() {
				@Override
				public void success(@Nullable byte[] replyPayload) {
					String message = replyPayload == null ? null : new String(replyPayload);
					testbedRuntime.getUnreliableMessagingService()
							.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
									buildRequestStatus(1, message)
							)
							);
				}

				@Override
				public void failure(byte responseType, @Nullable byte[] replyPayload) {
					testbedRuntime.getUnreliableMessagingService()
							.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
									buildRequestStatus(responseType, new String(replyPayload))
							)
							);
				}

				@Override
				public void timeout() {
					testbedRuntime.getUnreliableMessagingService()
							.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
									buildRequestStatus(0, "Communication to node timed out!")
							)
							);
				}
			}
			);

		}
	}

	private void executeSetVirtualLink(final WSNAppMessages.SetVirtualLinkRequest setVirtualLinkRequest,
									   final Messages.Msg msg) {

		Long destinationNode = null;
		try {

			String[] strings = setVirtualLinkRequest.getTargetNode().split(":");
			destinationNode = Long.parseLong(strings[strings.length - 1]);

		} catch (Exception e) {

			log.warn("Received destinationNode URN whose suffix could not be parsed to long: {}",
					setVirtualLinkRequest.getTargetNode()
			);
			testbedRuntime.getUnreliableMessagingService()
					.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
							buildRequestStatus(-1, "Destination node URN suffix is not a valid long value!")
					)
					);

		}

		if (destinationNode != null) {

			nodeApi.getLinkControl().setVirtualLink(destinationNode, new NodeApiCallback() {
				@Override
				public void success(@Nullable byte[] replyPayload) {
					String message = replyPayload == null ? null : new String(replyPayload);
					testbedRuntime.getUnreliableMessagingService()
							.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
									buildRequestStatus(1, message)
							)
							);
				}

				@Override
				public void failure(byte responseType, @Nullable byte[] replyPayload) {
					testbedRuntime.getUnreliableMessagingService()
							.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
									buildRequestStatus(responseType, new String(replyPayload))
							)
							);
				}

				@Override
				public void timeout() {
					testbedRuntime.getUnreliableMessagingService()
							.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
									buildRequestStatus(0, "Communication to node timed out!")
							)
							);
				}
			}
			);

		}
	}

	private void executeSendMessage(WSNAppMessages.Message message, Messages.Msg msg) {

		log.debug("WSNDeviceAppImpl.executeSendMessage({}, {})", message, msg);

		byte[] messageBytes = null;
		int messageType = -1;

		if (message.hasBinaryMessage()) {

			log.debug("Delivering binary message");
			WSNAppMessages.Message.BinaryMessage binaryMessage = message.getBinaryMessage();

			if (!binaryMessage.hasBinaryType()) {

				/*byte[] requestStatusBytes = buildRequestStatus(0, "Message type missing");
				testbedRuntime.getUnreliableMessagingService()
						.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
								requestStatusBytes
						)
						);
				*/
				log.warn("Message type missing in message {}", message);
				return;

			} else {

				messageType = binaryMessage.getBinaryType();
				messageBytes = binaryMessage.getBinaryData().toByteArray();

			}


		} else if (message.hasTextMessage()) {

			log.debug("Delivering text message");
			WSNAppMessages.Message.TextMessage textMessage = message.getTextMessage();

			messageType = textMessage.getMessageLevel().getNumber(); // TODO !?
			messageBytes = textMessage.getMsg().getBytes(); // TODO !?

		} else {

			log.error("This case MUST NOT OCCUR or something is wrong!!!!!!!!!!!!!!!");

		}

		MessagePacket p = new MessagePacket(messageType, messageBytes);
		try {

			iSenseDevice.send(p);
			log.debug("Sent message {} to node", p);

			// send "ack" to reliable messaging
			/*testbedRuntime.getUnreliableMessagingService()
					.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_ACK,
							buildRequestStatus(1, null)
					)
					);
			*/

		} catch (Exception e) {
			log.error("" + e, e);
			/*byte[] requestStatusBytes = buildRequestStatus(0, "Exception while delivering binary message to node");
			testbedRuntime.getUnreliableMessagingService()
					.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
							requestStatusBytes
					)
					);
			*/
		}

	}

	private void executeResetNodes(Messages.Msg msg, WSNAppMessages.OperationInvocation invocation) {

		// TODO check if operation is running

		log.debug("WSNDeviceAppImpl.executeResetNodes({})", msg);
		try {

			currentOperationInvocation = invocation;
			currentOperationInvocationMsg = msg;
			boolean triggered = iSenseDevice.triggerReboot();
			if (!triggered) {
				testbedRuntime.getUnreliableMessagingService()
						.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
								buildRequestStatus(0, "Unable to trigger reboot")
						)
						);
			}

		} catch (Exception e) {
			e.printStackTrace();  // TODO implement
		}
	}

	private void executeFlashPrograms(WSNAppMessages.OperationInvocation invocation,
									  SingleRequestMultiResponseListener.Responder responder) {

		log.debug("WSNDeviceAppImpl.executeFlashPrograms()");

		// TODO check if other operation is running

		try {

			WSNAppMessages.Program program = WSNAppMessages.Program.parseFrom(invocation.getArguments());

			JennicBinFile jennicBinFile =
					new JennicBinFile(program.getProgram().toByteArray(), program.getMetaData().toString());
			try {

				// remember invocation message to be able to send asynchronous replies
				currentOperationInvocation = invocation;
				currentOperationResponder = responder;
				currentOperationLastProgress = new TimeDiff(1000);

				if (!iSenseDevice.triggerProgram(jennicBinFile, true)) {
					failedFlashPrograms();
				}

			} catch (Exception e) {
				e.printStackTrace();  // TODO implement
			}


		} catch (InvalidProtocolBufferException e) {
			log.warn("Couldn't parse program for flash operation: {}. Ignoring...", e);
			return;
		}

	}

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

	private void failedFlashPrograms() {

		log.debug("WSNDeviceAppImpl.failedFlashPrograms()");

		// send reply to indicate failure
		currentOperationResponder.sendResponse(buildRequestStatus(-1, "Failed flashing node"));
		resetCurrentOperation();
	}

	private void resetCurrentOperation() {

		currentOperationInvocation = null;
		currentOperationInvocationMsg = null;
		currentOperationResponder = null;
		currentOperationLastProgress = null;

	}

	private void progressFlashPrograms(float value) {

		log.debug("WSNDeviceAppImpl.progressFlashPrograms({})", value);

		if (currentOperationLastProgress.isTimeout()) {

			log.debug("Sending asynchronous receivedRequestStatus message.");
			// send reply to indicate failure
			currentOperationResponder.sendResponse(buildRequestStatus((int) (value * 100), null));
			currentOperationLastProgress.touch();

		}

	}

	private void doneFlashPrograms() {

		log.debug("WSNDeviceAppImpl.doneFlashPrograms()");

		// send reply to indicate failure
		currentOperationResponder.sendResponse(buildRequestStatus(100, null));
		resetCurrentOperation();

	}

	private void executeAreNodesAlive(WSNAppMessages.OperationInvocation invocation, Messages.Msg msg) {

		log.debug("WSNDeviceAppImpl.executeAreNodesAlive({})", msg);

		// TODO really check if not is alive, e.g. through pinging
		testbedRuntime.getUnreliableMessagingService()
				.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
						buildRequestStatus(1, null)
				)
				);

	}

	private WSNAppMessages.OperationInvocation parseOperation(Messages.Msg msg) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Received operation invocation, bytes: {}",
						StringUtils.toHexString(msg.getPayload().toByteArray())
				);
			}
			return WSNAppMessages.OperationInvocation.parseFrom(msg.getPayload());
		} catch (InvalidProtocolBufferException e) {
			log.warn("Couldn't parse operation invocation message: {}. Ignoring...", e);
			return null;
		}
	}

	private boolean isFlashOperation(WSNAppMessages.OperationInvocation operationInvocation) {
		return operationInvocation != null && operationInvocation
				.getOperation() == WSNAppMessages.OperationInvocation.Operation.FLASH_PROGRAMS;
	}

	private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

	private static final byte NODE_OUTPUT_TEXT = 50;

	private static final byte NODE_OUTPUT_BYTE = 51;

	private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

	private iSenseDeviceListener iSenseDeviceListener = new iSenseDeviceListenerAdapter() {

		@Override
		public void receivePacket(MessagePacket p) {

			log.debug("WSNDeviceAppImpl.receivePacket: {}", p);

			boolean isWiselibUpstream = p.getType() == MESSAGE_TYPE_WISELIB_UPSTREAM;
			// TODO put this inside NodeAPI
			boolean isNodeOutput = isWiselibUpstream && (
					(p.getContent()[0] & 0xFF) == NODE_OUTPUT_BYTE ||
							(p.getContent()[0] & 0xFF) == NODE_OUTPUT_TEXT ||
							(p.getContent()[0] & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK
			);

			if (isWiselibUpstream && !isNodeOutput) {
				nodeApiDeviceAdapter.receiveFromNode(ByteBuffer.wrap(p.getContent()));
			} else {
				hackyDeliverToNodeMessageReceivers(p);
			}

		}

		@Override
		public void operationCanceled(Operation operation) {

			log.debug("Operation {} canceled.", operation);

			if (isFlashOperation(currentOperationInvocation) && operation == Operation.PROGRAM) {
				failedFlashPrograms();
			} else if (isResetOperation(currentOperationInvocation) && operation == Operation.RESET) {
				failedReset();
			}
		}

		@Override
		public void operationDone(Operation operation, Object o) {

			log.debug("Operation {} done. Object: {}", operation, o);

			if (isFlashOperation(currentOperationInvocation) && operation == Operation.PROGRAM) {
				doneFlashPrograms();
			} else if (isResetOperation(currentOperationInvocation) && operation == Operation.RESET) {
				doneReset();
			}
		}

		@Override
		public void operationProgress(Operation operation, float v) {

			log.debug("Operation {} receivedRequestStatus: {}", operation, v);

			if (isFlashOperation(currentOperationInvocation)) {
				progressFlashPrograms(v);
			}

		}

	};

	private void hackyDeliverToNodeMessageReceivers(MessagePacket p) {

		log.debug("WSNDeviceAppImpl.hackyDeliverToNodeMessageReceivers({})", p);

		if (hackyNodeMessageListeners.size() == 0) {
			log.warn("No message listeners registered!");
			return;
		}

		try {

			XMLGregorianCalendar now = DatatypeFactory.newInstance()
					.newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance());

			WSNAppMessages.Message.Builder messageBuilder = WSNAppMessages.Message.newBuilder()
					.setSourceNodeId(nodeUrn)
					.setTimestamp(now.toXMLFormat());

			boolean isTextMessage = PacketTypes.LOG == p.getType();

			if (isTextMessage) {

				byte[] content = p.getContent();

				if (content != null && content.length > 1) {

					WSNAppMessages.Message.MessageLevel messageLevel = null;

					switch (content[0]) {
						case PacketTypes.LogType.FATAL:
							messageLevel = WSNAppMessages.Message.MessageLevel.FATAL;
							break;
						default:
							messageLevel = WSNAppMessages.Message.MessageLevel.DEBUG;
							break;
					}

					String textMessage = new String(content, 1, content.length - 1);

					WSNAppMessages.Message.TextMessage.Builder textMessageBuilder =
							WSNAppMessages.Message.TextMessage.newBuilder()
									.setMessageLevel(messageLevel)
									.setMsg(textMessage);

					messageBuilder.setTextMessage(textMessageBuilder);

				} else {
					log.debug("Received text message without content. Ingoring packet: {}", p);
					return;
				}

			} else {

				WSNAppMessages.Message.BinaryMessage.Builder binaryMessageBuilder =
						WSNAppMessages.Message.BinaryMessage.newBuilder()
								.setBinaryType(p.getType())
								.setBinaryData(ByteString.copyFrom(p.getContent()));

				messageBuilder.setBinaryMessage(binaryMessageBuilder);

			}

			for (String nodeMessageListener : hackyNodeMessageListeners) {

				log.debug("Delivering node output to {}", nodeMessageListener);

				testbedRuntime.getUnreliableMessagingService()
						.sendAsync(nodeUrn, nodeMessageListener, WSNApp.MSG_TYPE_LISTENER_MESSAGE,
								messageBuilder.build().toByteArray(), 1, System.currentTimeMillis() + 5000
						);
			}

		} catch (DatatypeConfigurationException e) {
			log.error("" + e, e);
		}


	}

	private void doneReset() {

		log.debug("WSNDeviceAppImpl.doneReset()");

		// send reply to indicate failure
		testbedRuntime.getUnreliableMessagingService().sendAsync(
				MessageTools.buildReply(currentOperationInvocationMsg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
						buildRequestStatus(1, null)
				)
		);
		resetCurrentOperation();
	}

	private void failedReset() {

		log.debug("WSNDeviceAppImpl.failedReset()");

		// send reply to indicate failure
		testbedRuntime.getUnreliableMessagingService().sendAsync(
				MessageTools.buildReply(currentOperationInvocationMsg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
						buildRequestStatus(0, "Failed resetting node")
				)
		);
		resetCurrentOperation();
	}

	private boolean isResetOperation(WSNAppMessages.OperationInvocation operationInvocation) {
		return operationInvocation != null && operationInvocation
				.getOperation() == WSNAppMessages.OperationInvocation.Operation.RESET_NODES;
	}

	@Override
	public String getName() {
		return WSNDeviceApp.class.getSimpleName();
	}

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
				log.warn("Error while parsing operation invocation. Ignoring...: " + e, e);
			}

		}
	};

	@Override
	public void start() throws Exception {

		log.debug("WSNDeviceAppImpl.start()");

		// first connect to device
		iSenseDevice.registerListener(iSenseDeviceListener);

		// now start listening to messages
		testbedRuntime.getSingleRequestMultiResponseService()
				.addListener(nodeUrn, WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST, srmrsListener);
		testbedRuntime.getMessageEventService().addListener(messageEventListener);
	}

	@Override
	public void stop() {

		log.debug("WSNDeviceAppImpl.stop()");

		// first stop listening to messages
		testbedRuntime.getMessageEventService().removeListener(messageEventListener);
		testbedRuntime.getSingleRequestMultiResponseService().removeListener(srmrsListener);

		// then disconnect from device
		iSenseDevice.deregisterListener(iSenseDeviceListener);
	}

	public boolean isExclusiveOperationRunning() {
		// TODO nicer one...
		return currentOperationInvocation != null;
	}
}
