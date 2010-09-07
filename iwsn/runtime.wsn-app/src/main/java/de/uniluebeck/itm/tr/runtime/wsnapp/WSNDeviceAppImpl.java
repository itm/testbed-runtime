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
import de.uniluebeck.itm.motelist.AbstractMoteList;
import de.uniluebeck.itm.motelist.MoteListLinux;
import de.uniluebeck.itm.tr.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallback;
import de.uniluebeck.itm.tr.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicBinFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Singleton
class WSNDeviceAppImpl implements WSNDeviceApp {

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceApp.class);

	private String nodeUrn;

	private String nodeType;

	private String nodeSerialInterface;

	private TestbedRuntime testbedRuntime;

	private MessageEventListener messageEventListener = new MessageEventAdapter() {
		@Override
		public void messageReceived(Messages.Msg msg) {

			Preconditions.checkNotNull(iSenseDevice, "We should only receive message if we're connected to a device");

			boolean isRecipient = nodeUrn.equals(msg.getTo());
			boolean isOperationInvocation = WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST.equals(msg.getMsgType());
			boolean isListenerManagement = WSNApp.MSG_TYPE_LISTENER_MANAGEMENT.equals(msg.getMsgType());

			if (isRecipient && isOperationInvocation) {

				log.trace("{} => Received message of type {}...", nodeUrn, msg.getMsgType());

				WSNAppMessages.OperationInvocation invocation = parseOperation(msg);

				if (invocation != null && !isExclusiveOperationRunning()) {
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

	private final Set<String> nodeMessageListeners = new HashSet<String>();

	private NodeApiDeviceAdapter nodeApiDeviceAdapter = new NodeApiDeviceAdapter() {
		@Override
		public void sendToNode(final ByteBuffer packet) {
			try {
				if (log.isDebugEnabled()) {
					log.debug(
							"{} => Sending a WISELIB_DOWNSTREAM packet: {}",
							nodeUrn,
							StringUtils.toHexString(packet.array())
					);
				}
				iSenseDevice.send(new MessagePacket(MESSAGE_TYPE_WISELIB_DOWNSTREAM, packet.array()));
			} catch (Exception e) {
				log.error("" + e, e);
			}
		}
	};

	private NodeApi nodeApi;

	private static final int DEFAULT_NODE_API_TIMEOUT = 5000;

	private void executeManagement(WSNAppMessages.ListenerManagement management) {
		if (WSNAppMessages.ListenerManagement.Operation.REGISTER == management.getOperation()) {
			log.debug("{} => Node {} registered for node outputs", nodeUrn, management.getNodeName());
			nodeMessageListeners.add(management.getNodeName());
		} else {
			log.debug("{} => Node {} unregistered from node outputs", nodeUrn, management.getNodeName());
			nodeMessageListeners.remove(management.getNodeName());
		}
	}

	private Messages.Msg currentOperationInvocationMsg;

	private WSNAppMessages.OperationInvocation currentOperationInvocation;

	private TimeDiff currentOperationLastProgress;

	private iSenseDevice iSenseDevice;

	private SingleRequestMultiResponseListener.Responder currentOperationResponder;

	@Inject
	public WSNDeviceAppImpl(@Named(WSNDeviceAppModule.NAME_NODE_URN) String nodeUrn,
							@Named(WSNDeviceAppModule.NAME_NODE_TYPE) String nodeType,
							@Named(WSNDeviceAppModule.NAME_SERIAL_INTERFACE) @Nullable String nodeSerialInterface,
							@Named(WSNDeviceAppModule.NAME_NODE_API_TIMEOUT) @Nullable Integer nodeAPITimeout,
							TestbedRuntime testbedRuntime) {

		Preconditions.checkNotNull(nodeUrn);
		Preconditions.checkNotNull(nodeType);

		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeSerialInterface = nodeSerialInterface;
		this.testbedRuntime = testbedRuntime;
		this.nodeApi =
				new NodeApi(nodeApiDeviceAdapter, nodeAPITimeout == null ? DEFAULT_NODE_API_TIMEOUT : nodeAPITimeout,
						TimeUnit.MILLISECONDS
				);

		try {
			this.datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			log.error(nodeUrn + " => " + e, e);
		}
	}

	private void executeOperation(WSNAppMessages.OperationInvocation invocation, Messages.Msg msg) {

		switch (invocation.getOperation()) {

			case ARE_NODES_ALIVE:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> checkAreNodesAlive()", nodeUrn);
				executeAreNodesAlive(msg);
				break;

			case RESET_NODES:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> resetNodes()", nodeUrn);
				executeResetNodes(msg, invocation);
				break;

			case SEND:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> send()", nodeUrn);
				try {
					WSNAppMessages.Message message = WSNAppMessages.Message.parseFrom(invocation.getArguments());
					executeSendMessage(message);
				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for send operation: {}. Ignoring...", nodeUrn, e);
					return;
				}
				break;

			case SET_VIRTUAL_LINK:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> setVirtualLink()", nodeUrn);
				try {
					WSNAppMessages.SetVirtualLinkRequest setVirtualLinkRequest =
							WSNAppMessages.SetVirtualLinkRequest.parseFrom(invocation.getArguments());
					executeSetVirtualLink(setVirtualLinkRequest, msg);
				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for setVirtualLink operation: {}. Ignoring...", nodeUrn, e);
					return;
				}
				break;

			case DESTROY_VIRTUAL_LINK:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> destroyVirtualLink()", nodeUrn);
				try {
					WSNAppMessages.DestroyVirtualLinkRequest destroyVirtualLinkRequest =
							WSNAppMessages.DestroyVirtualLinkRequest.parseFrom(invocation.getArguments());
					executeDestroyVirtualLink(destroyVirtualLinkRequest, msg);
				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for setVirtualLink operation: {}. Ignoring...", nodeUrn, e);
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

			log.warn(
					"{} => Received destinationNode URN whose suffix could not be parsed to long: {}",
					nodeUrn,
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

			log.warn("{} => Received destinationNode URN whose suffix could not be parsed to long: {}",
					nodeUrn,
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

	private void executeSendMessage(WSNAppMessages.Message message) {

		log.debug("{} => WSNDeviceAppImpl.executeSendMessage()", nodeUrn);

		byte[] messageBytes = null;
		byte messageType = -1;

		if (message.hasBinaryMessage()) {

			WSNAppMessages.Message.BinaryMessage binaryMessage = message.getBinaryMessage();

			if (!binaryMessage.hasBinaryType()) {

				log.warn("{} => Message type missing in message {}", nodeUrn, message);
				return;

			} else {

				messageType = (byte) binaryMessage.getBinaryType();
				messageBytes = binaryMessage.getBinaryData().toByteArray();

				if (log.isDebugEnabled()) {
					log.debug("{} => Delivering binary message of type {} and payload {}", new Object[]{
							nodeUrn, StringUtils.toHexString(messageType), StringUtils.toHexString(messageBytes)
					}
					);
				}

			}


		} else if (message.hasTextMessage()) {

			log.debug("{} => Delivering text message \"{}\"", message.getTextMessage());
			WSNAppMessages.Message.TextMessage textMessage = message.getTextMessage();

			messageType = (byte) textMessage.getMessageLevel().getNumber();
			messageBytes = textMessage.getMsg().getBytes();

		} else {

			log.error("{} => This case MUST NOT OCCUR or something is wrong!!!!!!!!!!!!!!!", nodeUrn);

		}

		try {

			if (messageType == MESSAGE_TYPE_WISELIB_DOWNSTREAM && messageBytes[0] == VIRTUAL_LINK_MESSAGE) {

				log.debug("{} => Delivering virtual link message over node API", nodeUrn);

				ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);

				final byte RSSI = messageBuffer.get(2);
				final byte LQI = messageBuffer.get(3);
				final byte payloadLength = messageBuffer.get(4);
				final long destination = messageBuffer.getLong(5);
				final long source = messageBuffer.getLong(13);
				final byte[] payload = new byte[payloadLength];
				System.arraycopy(messageBytes, 21, payload, 0, payloadLength);

				final byte[] finalMessageBytes = messageBytes;

				System.out.println("payloadLength = " + payloadLength);

				nodeApi.getInteraction()
						.sendVirtualLinkMessage(RSSI, LQI, destination, source, payload, new NodeApiCallback() {
							@Override
							public void success(@Nullable final byte[] replyPayload) {
								log.debug(
										"{} => Successfully delivered virtual link message to node. MessageBytes: {}. Reply: {}",
										new Object[]{
												nodeUrn,
												StringUtils.toHexString(finalMessageBytes),
												StringUtils.toHexString(replyPayload)
										}
								);
							}

							@Override
							public void failure(final byte responseType, @Nullable final byte[] replyPayload) {
								log.warn(
										"{} => Failed to deliver virtual link message to node. ResponseType: {}. ReplyPayload: {}",
										new Object[]{
												nodeUrn,
												StringUtils.toHexString(responseType),
												StringUtils.toHexString(replyPayload)
										}
								);
							}

							@Override
							public void timeout() {
								log.warn("{} => Timed out when trying to deliver virtual link message to node.",
										nodeUrn
								);
							}
						}
						);
			} else {

				log.debug(
						"{} => Delivering message directly over iSenseDevice.send(), i.e. not as a virtual link message.",
						nodeUrn
				);
				iSenseDevice.send(new MessagePacket(messageType, messageBytes));

			}

		} catch (Exception e) {
			log.error("" + e, e);
		}

	}

	private void executeResetNodes(Messages.Msg msg, WSNAppMessages.OperationInvocation invocation) {

		log.debug("{} => WSNDeviceAppImpl.executeResetNodes()", nodeUrn);

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
			log.error("Error while resetting device: " + e, e);
		}

	}

	private void executeFlashPrograms(WSNAppMessages.OperationInvocation invocation,
									  SingleRequestMultiResponseListener.Responder responder) {

		log.debug("{} => WSNDeviceAppImpl.executeFlashPrograms()", nodeUrn);

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
					failedFlashPrograms("Failed to trigger programming.");
				}

			} catch (Exception e) {
				log.error("{} => Error while flashing device. Reason: {}", nodeUrn, e.getMessage());
			}


		} catch (InvalidProtocolBufferException e) {
			log.warn("{} => Couldn't parse program for flash operation: {}. Ignoring...", nodeUrn, e);
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

	private void failedFlashPrograms(String reason) {

		log.debug("{} => WSNDeviceAppImpl.failedFlashPrograms()", nodeUrn);

		// send reply to indicate failure
		currentOperationResponder.sendResponse(buildRequestStatus(-1, "Failed flashing node. Reason: " + reason));
		resetCurrentOperation();
	}

	private void resetCurrentOperation() {

		currentOperationInvocation = null;
		currentOperationInvocationMsg = null;
		currentOperationResponder = null;
		currentOperationLastProgress = null;

	}

	private void progressFlashPrograms(float value) {

		log.debug("{} => WSNDeviceAppImpl.progressFlashPrograms({})", nodeUrn, value);

		if (currentOperationLastProgress.isTimeout()) {

			log.debug("{} => Sending asynchronous receivedRequestStatus message.", nodeUrn);
			// send reply to indicate failure
			currentOperationResponder.sendResponse(buildRequestStatus((int) (value * 100), null));
			currentOperationLastProgress.touch();

		}

	}

	private void doneFlashPrograms() {

		log.debug("{} => WSNDeviceAppImpl.doneFlashPrograms()", nodeUrn);

		// send reply to indicate failure
		currentOperationResponder.sendResponse(buildRequestStatus(100, null));
		resetCurrentOperation();

	}

	private void executeAreNodesAlive(Messages.Msg msg) {

		log.debug("{} => WSNDeviceAppImpl.executeAreNodesAlive()", nodeUrn);

		// to the best of our knowledge, a node is alive if we're connected to it
		boolean connected = iSenseDevice != null;
		testbedRuntime.getUnreliableMessagingService()
				.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
						buildRequestStatus(connected ? 1 : 0, null)
				)
				);

	}

	private WSNAppMessages.OperationInvocation parseOperation(Messages.Msg msg) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("{} => Received operation invocation, bytes: {}",
						nodeUrn,
						StringUtils.toHexString(msg.getPayload().toByteArray())
				);
			}
			return WSNAppMessages.OperationInvocation.parseFrom(msg.getPayload());
		} catch (InvalidProtocolBufferException e) {
			log.warn("{} => Couldn't parse operation invocation message: {}. Ignoring...", nodeUrn, e);
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

	private static final byte VIRTUAL_LINK_MESSAGE = 11;

	private iSenseDeviceListener iSenseDeviceListener = new iSenseDeviceListenerAdapter() {

		@Override
		public void receivePacket(MessagePacket p) {

			log.trace("{} => WSNDeviceAppImpl.receivePacket: {}", nodeUrn, p);

			boolean isWiselibUpstream = p.getType() == MESSAGE_TYPE_WISELIB_UPSTREAM;
			boolean isByteTextOrVLink =
					(p.getContent()[0] & 0xFF) == NODE_OUTPUT_BYTE ||
							(p.getContent()[0] & 0xFF) == NODE_OUTPUT_TEXT ||
							(p.getContent()[0] & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK;

			boolean isWiselibReply = isWiselibUpstream && !isByteTextOrVLink;

			if (isWiselibReply) {
				if (log.isDebugEnabled()) {
					log.debug("{} => Received WISELIB_UPSTREAM packet with content: {}", nodeUrn, p);
				}
				nodeApiDeviceAdapter.receiveFromNode(ByteBuffer.wrap(p.getContent()));
			} else {
				deliverToNodeMessageReceivers(p);
			}

		}

		@Override
		public void operationCanceled(Operation operation) {

			log.debug("{} => Operation {} canceled.", nodeUrn, operation);

			if (isFlashOperation(currentOperationInvocation) && operation == Operation.PROGRAM) {
				failedFlashPrograms("operation canceled");
			} else if (isResetOperation(currentOperationInvocation) && operation == Operation.RESET) {
				failedReset();
			}
		}

		@Override
		public void operationDone(Operation operation, Object o) {

			log.debug("{} => Operation {} done. Object: {}", new Object[]{nodeUrn, operation, o});

			if (isFlashOperation(currentOperationInvocation) && operation == Operation.PROGRAM) {
				if (o instanceof Exception) {
					failedFlashPrograms(((Exception) o).getMessage());
				} else {
					doneFlashPrograms();
				}
			} else if (isResetOperation(currentOperationInvocation) && operation == Operation.RESET) {
				doneReset();
			}
		}

		@Override
		public void operationProgress(Operation operation, float v) {

			log.debug("{} => Operation {} receivedRequestStatus: {}", new Object[]{nodeUrn, operation, v});

			if (isFlashOperation(currentOperationInvocation)) {
				progressFlashPrograms(v);
			}

		}

	};

	private DatatypeFactory datatypeFactory = null;

	private void deliverToNodeMessageReceivers(MessagePacket p) {

		if (nodeMessageListeners.size() == 0) {
			log.debug("{} => No message listeners registered!", nodeUrn);
			return;
		}


		XMLGregorianCalendar now =
				datatypeFactory.newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance());

		WSNAppMessages.Message.Builder messageBuilder = WSNAppMessages.Message.newBuilder()
				.setSourceNodeId(nodeUrn)
				.setTimestamp(now.toXMLFormat());

		boolean isTextMessage = PacketTypes.LOG == p.getType();

		if (isTextMessage) {

			byte[] content = p.getContent();

			if (content != null && content.length > 1) {

				WSNAppMessages.Message.MessageLevel messageLevel;

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
				log.debug("{} => Received text message without content. Ignoring packet: {}", nodeUrn, p);
				return;
			}

		} else {

			WSNAppMessages.Message.BinaryMessage.Builder binaryMessageBuilder =
					WSNAppMessages.Message.BinaryMessage.newBuilder()
							.setBinaryType(p.getType())
							.setBinaryData(ByteString.copyFrom(p.getContent()));

			messageBuilder.setBinaryMessage(binaryMessageBuilder);

		}

		WSNAppMessages.Message message = messageBuilder.build();

		for (String nodeMessageListener : nodeMessageListeners) {

			if (log.isDebugEnabled()) {
				log.debug("{} => Delivering node output to {}: {}", new String[]{
						nodeUrn,
						nodeMessageListener,
						WSNAppMessageTools.toString(message, false)
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

	private void doneReset() {

		log.debug("{} => WSNDeviceAppImpl.doneReset()", nodeUrn);

		// send reply to indicate failure
		testbedRuntime.getUnreliableMessagingService().sendAsync(
				MessageTools.buildReply(currentOperationInvocationMsg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
						buildRequestStatus(1, null)
				)
		);
		resetCurrentOperation();
	}

	private void failedReset() {

		log.debug("{} => WSNDeviceAppImpl.failedReset()", nodeUrn);

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
				log.warn("{} => Error while parsing operation invocation. Ignoring...: {}", nodeUrn, e);
			}

		}
	};

	private Runnable connectRunnable = new Runnable() {
		@Override
		public void run() {

			if (nodeSerialInterface == null || "".equals(nodeSerialInterface)) {

				Long macAddress = StringUtils.parseHexOrDecLongFromUrn(nodeUrn);
				AbstractMoteList moteList;

				log.debug("{} => Using motelist module to detect serial port for {} device.", nodeType, nodeUrn);

				try {
					moteList = new MoteListLinux();
				} catch (IOException e) {
					log.error(
							"{} => Failed to load the motelist module to detect the serial port. Reason: {}. Not trying to reconnect to device.",
							nodeUrn,
							e.getMessage()
					);
					return;
				}

				nodeSerialInterface = moteList.getMotePort(nodeType, macAddress);

				if (nodeSerialInterface == null) {
					log.warn("{} => No serial interface could be detected for {} mote. Retrying in 30 seconds.",
							nodeUrn, nodeType
					);
					testbedRuntime.getSchedulerService().schedule(this, 30, TimeUnit.SECONDS);
					return;
				}

			}

			try {

				iSenseDevice = DeviceFactory.create(nodeType, nodeSerialInterface);

			} catch (Exception e) {
				log.warn("{} => Connection to {} device on serial port {} failed. Reason: {}. Retrying in 30 seconds.",
						new Object[]{
								nodeUrn, nodeType, nodeSerialInterface, e.getMessage()
						}
				);
				testbedRuntime.getSchedulerService().schedule(this, 30, TimeUnit.SECONDS);
				return;
			}

			// attach as listener to device output
			iSenseDevice.registerListener(iSenseDeviceListener);

			// now start listening to messages
			testbedRuntime.getSingleRequestMultiResponseService()
					.addListener(nodeUrn, WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST, srmrsListener);
			testbedRuntime.getMessageEventService().addListener(messageEventListener);

		}
	};

	@Override
	public void start() throws Exception {

		log.debug("{} => WSNDeviceAppImpl.start()", nodeUrn);

		testbedRuntime.getSchedulerService().execute(connectRunnable);
	}

	@Override
	public void stop() {

		log.debug("{} => WSNDeviceAppImpl.stop()", nodeUrn);


		// first stop listening to messages
		testbedRuntime.getMessageEventService().removeListener(messageEventListener);
		testbedRuntime.getSingleRequestMultiResponseService().removeListener(srmrsListener);

		// then disconnect from device
		if (iSenseDevice != null) {
			iSenseDevice.deregisterListener(iSenseDeviceListener);
			log.debug("{} => Shutting down iSenseDevice");
			iSenseDevice.shutdown();
		}
	}

	public boolean isExclusiveOperationRunning() {
		return currentOperationInvocation != null;
	}
}
