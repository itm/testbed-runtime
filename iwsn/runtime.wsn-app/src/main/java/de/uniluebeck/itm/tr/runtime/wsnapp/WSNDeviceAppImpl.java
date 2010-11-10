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
import de.uniluebeck.itm.motelist.MoteList;
import de.uniluebeck.itm.motelist.MoteListFactory;
import de.uniluebeck.itm.motelist.MoteType;
import de.uniluebeck.itm.tr.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallback;
import de.uniluebeck.itm.tr.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.nio.ByteBuffer;
import java.util.*;
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

			Preconditions.checkNotNull(device, "We should only receive message if we're connected to a device");

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
				device.send(new MessagePacket(MESSAGE_TYPE_WISELIB_DOWNSTREAM, packet.array()));
			} catch (Exception e) {
				log.error("" + e, e);
			}
		}
	};

	private NodeApi nodeApi;

	private static final int DEFAULT_NODE_API_TIMEOUT = 5000;

	private String nodeUSBChipID;

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

	private iSenseDevice device;

	private SingleRequestMultiResponseListener.Responder currentOperationResponder;

	private WSNDeviceAppConnector connector;

	@Inject
	public WSNDeviceAppImpl(@Named(WSNDeviceAppModule.NAME_NODE_URN) String nodeUrn,
							@Named(WSNDeviceAppModule.NAME_NODE_TYPE) String nodeType,
							@Named(WSNDeviceAppModule.NAME_SERIAL_INTERFACE) @Nullable String nodeSerialInterface,
							@Named(WSNDeviceAppModule.NAME_NODE_API_TIMEOUT) @Nullable Integer nodeAPITimeout,
							@Named(WSNDeviceAppModule.NAME_USB_CHIP_ID) @Nullable String nodeUSBChipID,
							TestbedRuntime testbedRuntime) {

		Preconditions.checkNotNull(nodeUrn);
		Preconditions.checkNotNull(nodeType);

		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeUSBChipID = nodeUSBChipID;
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

			case DISABLE_NODE:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> disableNode()", nodeUrn);
				connector.disableNode(new ReplyingNodeApiCallback(msg));
				break;

			case ENABLE_NODE:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> enableNode()", nodeUrn);
				connector.enableNode(new ReplyingNodeApiCallback(msg));
				break;

			case DISABLE_PHYSICAL_LINK:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> disablePhysicalLink()", nodeUrn);
				try {

					WSNAppMessages.DisablePhysicalLink disablePhysicalLink =
							WSNAppMessages.DisablePhysicalLink.parseFrom(invocation.getArguments());
					long nodeB = StringUtils.parseHexOrDecLongFromUrn(disablePhysicalLink.getNodeB());
					connector.disablePhysicalLink(nodeB, new ReplyingNodeApiCallback(msg));

				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for disablePhysicalLink operation: {}. Ignoring...", nodeUrn,
							e
					);
					return;
				} catch (NumberFormatException e) {
					log.warn("{} => Couldn't parse long value for disablePhysicalLink operation: {}. Ignoring...",
							nodeUrn, e
					);
					testbedRuntime.getUnreliableMessagingService()
							.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
									buildRequestStatus(-1, "Destination node is not a valid long value!")
							)
							);
				}
				break;

			case ENABLE_PHYSICAL_LINK:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> enablePhysicalLink()", nodeUrn);
				try {

					WSNAppMessages.EnablePhysicalLink enablePhysicalLink =
							WSNAppMessages.EnablePhysicalLink.parseFrom(invocation.getArguments());
					long nodeB = StringUtils.parseHexOrDecLongFromUrn(enablePhysicalLink.getNodeB());
					connector.enablePhysicalLink(nodeB, new ReplyingNodeApiCallback(msg));

				} catch (InvalidProtocolBufferException e) {
					log.warn("{} => Couldn't parse message for enablePhysicalLink operation: {}. Ignoring...", nodeUrn,
							e
					);
					return;
				} catch (NumberFormatException e) {
					log.warn("{} => Couldn't parse long value for enablePhysicalLink operation: {}. Ignoring...",
							nodeUrn, e
					);
					testbedRuntime.getUnreliableMessagingService()
							.sendAsync(MessageTools.buildReply(msg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
									buildRequestStatus(-1, "Destination node is not a valid long value!")
							)
							);
				}
				break;

			case RESET_NODES:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> resetNodes()", nodeUrn);
				executeResetNodes(msg, invocation);
				break;

			case SEND:
				log.trace("{} => WSNDeviceAppImpl.executeOperation --> send()", nodeUrn);
				try {
					WSNAppMessages.Message message = WSNAppMessages.Message.parseFrom(invocation.getArguments());
					executeSendMessage(msg, message);
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

	private class ReplyingNodeApiCallback implements NodeApiCallback {

		private Messages.Msg invocationMsg;

		private ReplyingNodeApiCallback(final Messages.Msg invocationMsg) {
			this.invocationMsg = invocationMsg;
		}

		@Override
		public void success(@Nullable byte[] replyPayload) {
			String message = replyPayload == null ? null : new String(replyPayload);
			sendExecutionReply(invocationMsg, 1, message);
		}

		@Override
		public void failure(byte responseType, @Nullable byte[] replyPayload) {
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

	public void executeDestroyVirtualLink(final WSNAppMessages.DestroyVirtualLinkRequest destroyVirtualLinkRequest,
										  final Messages.Msg msg) {

		Long destinationNode = null;
		try {

			String[] strings = destroyVirtualLinkRequest.getTargetNode().split(":");
			destinationNode = StringUtils.parseHexOrDecLong(strings[strings.length - 1]);

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
			connector.destroyVirtualLink(destinationNode, new ReplyingNodeApiCallback(msg));
		}
	}

	public void executeSetVirtualLink(final WSNAppMessages.SetVirtualLinkRequest setVirtualLinkRequest,
									  final Messages.Msg msg) {

		Long destinationNode = null;
		try {

			String[] strings = setVirtualLinkRequest.getTargetNode().split(":");
			destinationNode = StringUtils.parseHexOrDecLong(strings[strings.length - 1]);

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
			connector.setVirtualLink(destinationNode, new ReplyingNodeApiCallback(msg));
		}
	}

	public void executeSendMessage(final Messages.Msg msg, final WSNAppMessages.Message message) {

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

		connector.sendMessage(messageType, messageBytes, new ReplyingNodeApiCallback(msg));
	}

	public void executeResetNodes(Messages.Msg msg, WSNAppMessages.OperationInvocation invocation) {
		connector.resetNode(new ReplyingNodeApiCallback(msg));
	}

	@Override
	public void executeFlashPrograms(WSNAppMessages.OperationInvocation invocation,
									 SingleRequestMultiResponseListener.Responder responder) {

		try {
			WSNAppMessages.Program program = WSNAppMessages.Program.parseFrom(invocation.getArguments());
			connector.flashProgram(program, new WSNDeviceAppConnector.FlashProgramCallback() {
				@Override
				public void progress(final float percentage) {

					log.debug("{} => WSNDeviceAppImpl.progressFlashPrograms({})", nodeUrn, percentage);

					if (currentOperationLastProgress.isTimeout()) {

						log.debug("{} => Sending asynchronous receivedRequestStatus message.", nodeUrn);
						// send reply to indicate failure
						currentOperationResponder.sendResponse(buildRequestStatus((int) (percentage * 100), null));
						currentOperationLastProgress.touch();

					}

				}

				@Override
				public void success(@Nullable final byte[] replyPayload) {

					log.debug("{} => WSNDeviceAppImpl.doneFlashPrograms()", nodeUrn);

					// send reply to indicate failure
					currentOperationResponder.sendResponse(buildRequestStatus(100, null));
					resetCurrentOperation();
				}

				@Override
				public void failure(final byte responseType, @Nullable final byte[] replyPayload) {
					log.debug("{} => WSNDeviceAppImpl.failedFlashPrograms()", nodeUrn);

					// send reply to indicate failure
					currentOperationResponder.sendResponse(
							buildRequestStatus(-1, "Failed flashing node. Reason: " + new String(replyPayload))
					);
					resetCurrentOperation();
				}

				@Override
				public void timeout() {
					// TODO implement
				}
			}
			);
		} catch (InvalidProtocolBufferException e) {
			log.warn("{} => Couldn't parse program for flash operation: {}. Ignoring...", nodeUrn, e);
		}


	}

	public void executeAreNodesAlive(Messages.Msg msg) {
		connector.isNodeAlive(new ReplyingNodeApiCallback(msg));
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
		if (device != null) {
			device.deregisterListener(deviceListener);
			log.debug("{} => Shutting down {} device", nodeUrn, nodeType);
			device.shutdown();
		}
	}

}
