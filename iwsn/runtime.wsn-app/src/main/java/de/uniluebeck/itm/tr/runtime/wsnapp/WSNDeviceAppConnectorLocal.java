package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.inject.internal.Nullable;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.gtr.common.AbstractListenable;
import de.uniluebeck.itm.gtr.messaging.MessageTools;
import de.uniluebeck.itm.gtr.messaging.Messages;
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
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicBinFile;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicDevice;
import de.uniluebeck.itm.wsn.devicedrivers.pacemate.PacemateBinFile;
import de.uniluebeck.itm.wsn.devicedrivers.pacemate.PacemateDevice;
import de.uniluebeck.itm.wsn.devicedrivers.telosb.TelosbBinFile;
import de.uniluebeck.itm.wsn.devicedrivers.telosb.TelosbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class WSNDeviceAppConnectorLocal extends AbstractListenable<WSNDeviceAppConnector.NodeOutputListener>
		implements WSNDeviceAppConnector {

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceAppConnector.class);

	private static final int DEFAULT_NODE_API_TIMEOUT = 5000;

	private String nodeType;

	private String nodeUSBChipID;

	private String nodeSerialInterface;

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

	private String nodeUrn;

	private Messages.Msg currentOperationInvocationMsg;

	private TimeDiff currentOperationLastProgress;

	private iSenseDevice device;

	public WSNDeviceAppConnectorLocal(final String nodeUrn, final String nodeType, final String nodeUSBChipID,
									  final String nodeSerialInterface, final Integer nodeAPITimeout) {

		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeUSBChipID = nodeUSBChipID;
		this.nodeSerialInterface = nodeSerialInterface;

		this.nodeApi =
				new NodeApi(nodeApiDeviceAdapter, nodeAPITimeout == null ? DEFAULT_NODE_API_TIMEOUT : nodeAPITimeout,
						TimeUnit.MILLISECONDS
				);
	}

	@Override
	public void enablePhysicalLink(final long nodeB, final NodeApiCallback listener) {
		nodeApi.getLinkControl().disablePhysicalLink(nodeB, listener);
	}

	@Override
	public void disablePhysicalLink(final long nodeB, final NodeApiCallback listener) {
		nodeApi.getLinkControl().enablePhysicalLink(nodeB, listener);
	}

	@Override
	public void enableNode(final NodeApiCallback listener) {
		nodeApi.getNodeControl().enableNode(listener);
	}

	@Override
	public void disableNode(final NodeApiCallback listener) {
		nodeApi.getNodeControl().disableNode(listener);
	}

	@Override
	public void destroyVirtualLink(final long targetNode, final NodeApiCallback listener) {
		nodeApi.getLinkControl().destroyVirtualLink(targetNode, listener);
	}

	@Override
	public void setVirtualLink(final long targetNode, final NodeApiCallback listener) {
		nodeApi.getLinkControl().setVirtualLink(targetNode, listener);
	}

	@Override
	public void sendMessage(final byte binaryType, final byte[] binaryMessage, final NodeApiCallback listener) {

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
				device.send(new MessagePacket(messageType, messageBytes));

			}

		} catch (Exception e) {
			log.error("" + e, e);
		}

	}

	@Override
	public void resetNode(final NodeApiCallback listener) {

		log.debug("{} => WSNDeviceAppImpl.executeResetNodes()", nodeUrn);

		try {

			currentOperationInvocation = invocation;
			currentOperationInvocationMsg = msg;

			if (!device.isConnected()) {
				listener.failure((byte) 0, "Failed resetting node. Reason: Device is not connected.".getBytes());
			}

			boolean triggered = device.triggerReboot();
			if (!triggered) {
				listener.failure((byte) 0, "Failed resetting node. Reason: Could not trigger reboot.".getBytes());
			}

		} catch (Exception e) {
			log.error("Error while resetting device: " + e, e);
			listener.failure((byte) 0, ("Failed resetting node. Reason: " + e.getMessage()).getBytes());
		}

	}

	@Override
	public void isNodeAlive(final NodeApiCallback listener) {

		log.debug("{} => WSNDeviceAppImpl.executeAreNodesAlive()", nodeUrn);

		// to the best of our knowledge, a node is alive if we're connected to it
		boolean connected = device != null && device.isConnected();
		if (connected) {
			listener.success(null);
		} else {
			listener.failure((byte) 0, "Device is not connected".getBytes());
		}
	}

	@Override
	public void flashProgram(final WSNAppMessages.Program program, final FlashProgramCallback listener) {

		log.debug("{} => WSNDeviceAppImpl.executeFlashPrograms()", nodeUrn);

		try {

			IDeviceBinFile iSenseBinFile = null;

			if (device instanceof JennicDevice) {
				iSenseBinFile = new JennicBinFile(program.getProgram().toByteArray(), program.getMetaData().toString());
			} else if (device instanceof TelosbDevice) {
				iSenseBinFile = new TelosbBinFile(program.getProgram().toByteArray(), program.getMetaData().toString());
			} else if (device instanceof PacemateDevice) {
				iSenseBinFile =
						new PacemateBinFile(program.getProgram().toByteArray(), program.getMetaData().toString());
			}

			try {

				// remember invocation message to be able to send asynchronous replies
				currentOperationLastProgress = new TimeDiff(1000);

				if (!device.isConnected()) {
					listener.failure((byte) 0, "Failed flashing node. Reason: Node is not connected.".getBytes());
					return;
				}

				if (!device.triggerProgram(iSenseBinFile, true)) {
					listener.failure((byte) 0, "Failed to trigger programming.".getBytes());
					return;
				}

			} catch (Exception e) {
				log.error("{} => Error while flashing device. Reason: {}", nodeUrn, e.getMessage());
				listener.failure((byte) 0, ("Error while flashing device. Reason: " + e.getMessage()).getBytes();)
				return;
			}

		} catch (InvalidProtocolBufferException e) {
			log.warn("{} => Couldn't parse program for flash operation: {}. Ignoring...", nodeUrn, e);
		} catch (Exception e) {
			log.error("Error reading bin file " + e);
		}

	}

	private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

	private static final byte NODE_OUTPUT_TEXT = 50;

	private static final byte NODE_OUTPUT_BYTE = 51;

	private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

	private static final byte VIRTUAL_LINK_MESSAGE = 11;

	private iSenseDeviceListener deviceListener = new iSenseDeviceListenerAdapter() {

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
				notifyListeners(p);
			}

		}

		@Override
		public void operationCanceled(Operation operation) {

			log.debug("{} => Operation {} canceled.", nodeUrn, operation);

			if (isFlashOperation(currentOperationInvocation) && operation == Operation.PROGRAM) {
				failedFlashPrograms("operation canceled");
				resetCurrentOperation();
			} else if (isResetOperation(currentOperationInvocation) && operation == Operation.RESET) {
				failedReset("Failed resetting node. Reason: Operation canceled.");
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
					resetCurrentOperation();
				}
			} else if (isResetOperation(currentOperationInvocation) && operation == Operation.RESET) {
				if (o == null) {
					failedFlashPrograms("Could not reset node");
				} else if (o instanceof Boolean && ((Boolean) o).booleanValue()) {
					doneReset();
				} else {
					failedFlashPrograms("Could not reset node"
					);//urn:wisebed:uzl-staging:0xe301,urn:wisebed:uzl-staging:0x2504,urn:wisebed:uzl-staging:0x0d99,urn:wisebed:uzl-staging:0x2bbb
				}
			}
		}

		@Override
		public void operationProgress(Operation operation, float v) {

			log.debug("{} => Operation {} receivedRequestStatus: {}", new Object[]{nodeUrn, operation, v});

			if (isFlashOperation(currentOperationInvocation)) {

				if (currentOperationLastProgress.isTimeout()) {

					log.debug("{} => Sending asynchronous receivedRequestStatus message.", nodeUrn);
					// send reply to indicate failure
					currentOperationLastProgress.touch();
					// TODO arggh!

				}
			}

		}

	};

	private void notifyListeners(final MessagePacket p) {
		for (NodeOutputListener listener : listeners) {
			listener.receivedPacket(p);
		}
	}

	private boolean isFlashOperation(WSNAppMessages.OperationInvocation operationInvocation) {
		return operationInvocation != null && operationInvocation
				.getOperation() == WSNAppMessages.OperationInvocation.Operation.FLASH_PROGRAMS;
	}

	private boolean isResetOperation(WSNAppMessages.OperationInvocation operationInvocation) {
		return operationInvocation != null && operationInvocation
				.getOperation() == WSNAppMessages.OperationInvocation.Operation.RESET_NODES;
	}

	private void resetCurrentOperation() {

		currentOperationInvocation = null;
		currentOperationInvocationMsg = null;
		currentOperationResponder = null;
		currentOperationLastProgress = null;

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

	private void failedReset(String reason) {

		log.debug("{} => WSNDeviceAppImpl.failedReset()", nodeUrn);

		// send reply to indicate failure
		testbedRuntime.getUnreliableMessagingService().sendAsync(
				MessageTools.buildReply(currentOperationInvocationMsg, WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
						buildRequestStatus(0, reason)
				)
		);
		resetCurrentOperation();
	}

	private Runnable connectRunnable = new Runnable() {
		@Override
		public void run() {

			if (nodeSerialInterface == null || "".equals(nodeSerialInterface)) {

				Long macAddress = StringUtils.parseHexOrDecLongFromUrn(nodeUrn);
				MoteList moteList;

				log.debug("{} => Using motelist module to detect serial port for {} device.", nodeUrn, nodeType);

				try {
					Map<String, String> telosBReferenceToMACMap = null;
					if ("telosb".equals(nodeType) && nodeUSBChipID != null && !"".equals(nodeUSBChipID)) {
						telosBReferenceToMACMap = new HashMap<String, String>() {{
							put(nodeUSBChipID, StringUtils.getUrnSuffix(nodeUrn));
						}};
					}
					moteList = MoteListFactory.create(telosBReferenceToMACMap);
				} catch (Exception e) {
					log.error(
							"{} => Failed to load the motelist module to detect the serial port. Reason: {}. Not trying to reconnect to device.",
							nodeUrn,
							e.getMessage()
					);
					return;
				}

				try {
					nodeSerialInterface = moteList.getMotePort(MoteType.fromString(nodeType.toLowerCase()), macAddress);
				} catch (Exception e) {
					log.warn("{} => Exception while detecting serial interface: {}", nodeUrn, e);
				}

				if (nodeSerialInterface == null) {
					log.warn("{} => No serial interface could be detected for {} node. Retrying in 30 seconds.",
							nodeUrn, nodeType
					);
					testbedRuntime.getSchedulerService().schedule(this, 30, TimeUnit.SECONDS);
					return;
				} else {
					log.debug("{} => Found {} node on serial port {}.",
							new Object[]{nodeUrn, nodeType, nodeSerialInterface}
					);
				}

			}

			try {

				device = DeviceFactory.create(nodeType, nodeSerialInterface);

			} catch (Exception e) {
				log.warn("{} => Connection to {} device on serial port {} failed. Reason: {}. Retrying in 30 seconds.",
						new Object[]{
								nodeUrn, nodeType, nodeSerialInterface, e.getMessage()
						}
				);
				testbedRuntime.getSchedulerService().schedule(this, 30, TimeUnit.SECONDS);
				return;
			}

			log.debug("{} => Successfully connected to {} node on serial port {}",
					new Object[]{nodeUrn, nodeType, nodeSerialInterface}
			);

			// attach as listener to device output
			device.registerListener(deviceListener);

		}
	};

	public boolean isExclusiveOperationRunning() {
		return currentOperationInvocation != null;
	}

	@Override
	public void start() throws Exception {
		// TODO implement
	}

	@Override
	public void stop() {

		if (device != null) {
			device.deregisterListener(deviceListener);
			log.debug("{} => Shutting down {} device", nodeUrn, nodeType);
			device.shutdown();
		}

	}
}
