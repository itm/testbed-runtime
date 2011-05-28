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

import de.uniluebeck.itm.gtr.common.SchedulerService;
import de.uniluebeck.itm.motelist.MoteList;
import de.uniluebeck.itm.motelist.MoteListFactory;
import de.uniluebeck.itm.motelist.MoteType;
import de.uniluebeck.itm.rsc.drivers.core.Connection;
import de.uniluebeck.itm.rsc.drivers.core.Device;
import de.uniluebeck.itm.rsc.drivers.core.MessagePacket;
import de.uniluebeck.itm.rsc.drivers.core.MessagePacketListener;
import de.uniluebeck.itm.rsc.drivers.core.async.*;
import de.uniluebeck.itm.rsc.drivers.core.async.thread.PausableExecutorOperationQueue;
import de.uniluebeck.itm.rsc.drivers.core.event.MessageEvent;
import de.uniluebeck.itm.rsc.drivers.core.exception.ProgramChipMismatchException;
import de.uniluebeck.itm.rsc.drivers.core.mockdevice.MockConnection;
import de.uniluebeck.itm.rsc.drivers.core.mockdevice.MockDevice;
import de.uniluebeck.itm.rsc.drivers.core.operation.Operation;
import de.uniluebeck.itm.rsc.drivers.core.operation.ProgramOperation;
import de.uniluebeck.itm.rsc.drivers.core.serialport.SerialPortConnection;
import de.uniluebeck.itm.rsc.drivers.jennic.JennicDevice;
import de.uniluebeck.itm.rsc.drivers.jennic.JennicSerialPortConnection;
import de.uniluebeck.itm.rsc.drivers.pacemate.PacemateDevice;
import de.uniluebeck.itm.rsc.drivers.pacemate.PacemateSerialPortConnection;
import de.uniluebeck.itm.rsc.drivers.telosb.TelosbDevice;
import de.uniluebeck.itm.rsc.drivers.telosb.TelosbSerialPortConnection;
import de.uniluebeck.itm.tr.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class WSNDeviceAppConnectorLocalNew extends AbstractListenable<WSNDeviceAppConnector.NodeOutputListener>
		implements WSNDeviceAppConnector {

	private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

	private static final byte NODE_OUTPUT_TEXT = 50;

	private static final byte NODE_OUTPUT_BYTE = 51;

	private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

	private static final byte VIRTUAL_LINK_MESSAGE = 11;

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceAppConnector.class);

	private static final int DEFAULT_NODE_API_TIMEOUT = 1000;

	private static final int DEFAULT_MAXIMUM_MESSAGE_RATE = Integer.MAX_VALUE;

	private final String nodeType;

	private final String nodeUSBChipID;

	private String nodeSerialInterface;

	private final SchedulerService schedulerService;

	private int resetCount = 0;

	private int flashCount = 0;

	private final int timeoutReset;

	private final int timeoutFlash;

	private final int maximumMessageRate;

	private final TimeUnit maximumMessageRateTimeUnit;

	private final RateLimiter maximumMessageRateLimiter;

	private final Runnable connectRunnable = new Runnable() {


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
					schedulerService.schedule(this, 30, TimeUnit.SECONDS);
					return;
				} else {
					log.debug("{} => Found {} node on serial port {}.",
							new Object[]{nodeUrn, nodeType, nodeSerialInterface}
					);
				}

			}

			Connection tmpConnection;
			Device tmpDevice;

			if ("isense".equals(nodeType)) {
				tmpConnection = new JennicSerialPortConnection();
				tmpDevice = new JennicDevice((SerialPortConnection) tmpConnection);

			} else if ("pacemate".equals(nodeType)) {
				tmpConnection = new PacemateSerialPortConnection();
				tmpDevice = new PacemateDevice((SerialPortConnection) tmpConnection);

			} else if ("telosb".equals(nodeType)) {
				tmpConnection = new TelosbSerialPortConnection();
				tmpDevice = new TelosbDevice((SerialPortConnection) tmpConnection);

			} else if ("mock".equals(nodeType)) {
				tmpConnection = new MockConnection();
				tmpDevice = new MockDevice((MockConnection) tmpConnection);

			} else {
				throw new RuntimeException("Unsupported device type \"" + nodeType + "\"!");
			}

			try {
				tmpConnection.connect(nodeSerialInterface);
			} catch (Exception e) {
				log.warn("{} => Connection to {} device on serial port {} failed. Reason: {}. Retrying in 30 seconds.",
						new Object[]{
								nodeUrn, nodeType, nodeSerialInterface, e.getMessage()
						}
				);
				schedulerService.schedule(this, 30, TimeUnit.SECONDS);
				return;
			}

			log.debug("{} => Successfully connected to {} node on serial port {}",
					new Object[]{nodeUrn, nodeType, nodeSerialInterface}
			);

			deviceConnection = tmpConnection;
			deviceOperationQueue = new PausableExecutorOperationQueue();
			device = new QueuedDeviceAsync(deviceOperationQueue, tmpDevice);

			// attach as listener to device output
			device.addListener(messagePacketListener);

		}
	};

	private MessagePacketListener messagePacketListener = new MessagePacketListener() {
		@Override
		public void onMessagePacketReceived(final MessageEvent<MessagePacket> event) {

			MessagePacket p = event.getMessage();

			//if reaching maximum-message-rate do not send more then 1 message
			if (!maximumMessageRateLimiter.checkIfInSlotAndCount()) {
				int dismissedCount = maximumMessageRateLimiter.dismissedCount();
				if (dismissedCount >= 1) {
					sendPacketsDroppedWarningIfNotificationRateAllows(dismissedCount);
				}
				return;
			}

			log.trace("{} => WSNDeviceAppConnectorLocal.receivePacket: {}", nodeUrn, p);

			// convert to plain bytes
			byte[] bytes = new byte[1 + p.getContent().length];
			bytes[0] = (byte) (0xFF & p.getType());
			System.arraycopy(p.getContent(), 0, bytes, 1, p.getContent().length);

			boolean isWiselibUpstream = bytes[0] == MESSAGE_TYPE_WISELIB_UPSTREAM;
			boolean isByteTextOrVLink = (bytes[1] & 0xFF) == NODE_OUTPUT_BYTE ||
					(bytes[1] & 0xFF) == NODE_OUTPUT_TEXT ||
					(bytes[1] & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK;

			boolean isWiselibReply = isWiselibUpstream && !isByteTextOrVLink;

			if (isWiselibReply && nodeApiDeviceAdapter.receiveFromNode(ByteBuffer.wrap(p.getContent()))) {

				if (log.isDebugEnabled()) {
					log.debug("{} => Received WISELIB_UPSTREAM packet with content: {}", nodeUrn, p);
				}

			} else {
				for (NodeOutputListener listener : listeners) {
					listener.receivedPacket(bytes);
				}
			}

		}
	};

	private static final int PACKETS_DROPPED_NOTIFICATION_RATE = 1000;

	private final TimeDiff packetsDroppedTimeDiff = new TimeDiff(PACKETS_DROPPED_NOTIFICATION_RATE);

	private int packetsDroppedSinceLastNotification = 0;

	private void sendPacketsDroppedWarningIfNotificationRateAllows(int packetsDropped) {

		packetsDroppedSinceLastNotification += packetsDropped;

		if (packetsDroppedTimeDiff.isTimeout()) {

			String notification =
					"Dropped " + packetsDroppedSinceLastNotification + " packets of " + nodeUrn + " in the last "
							+ packetsDroppedTimeDiff.ms() + " milliseconds, because the node writes more packets to "
							+ "the serial interface per second than allowed (" + maximumMessageRate + " per " +
							maximumMessageRateTimeUnit + ").";

			for (NodeOutputListener listener : listeners) {
				listener.receiveNotification(notification);
			}

			packetsDroppedSinceLastNotification = 0;
			packetsDroppedTimeDiff.touch();
		}
	}

	private NodeApi nodeApi;

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

				OperationHandle<Void> handle = device.send(
						new MessagePacket(MESSAGE_TYPE_WISELIB_DOWNSTREAM, packet.array()),
						1000,
						new AsyncAdapter<Void>()
				);
				handle.get();

			} catch (Exception e) {
				log.error("" + e, e);
			}
		}
	};

	private String nodeUrn;

	private OperationQueue deviceOperationQueue;

	private Connection deviceConnection;

	private DeviceAsync device;

	public WSNDeviceAppConnectorLocalNew(final String nodeUrn, final String nodeType, final String nodeUSBChipID,
										 final String nodeSerialInterface, final Integer timeoutNodeAPI,
										 final Integer maximumMessageRate, final SchedulerService schedulerService,
										 final Integer timeoutReset, final Integer timeoutFlash) {

		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeUSBChipID = nodeUSBChipID;
		this.nodeSerialInterface = nodeSerialInterface;
		this.schedulerService = schedulerService;

		this.maximumMessageRate = (maximumMessageRate == null ? DEFAULT_MAXIMUM_MESSAGE_RATE : maximumMessageRate);
		this.maximumMessageRateTimeUnit = TimeUnit.SECONDS;
		this.maximumMessageRateLimiter = new RateLimiterImpl(this.maximumMessageRate, 1, maximumMessageRateTimeUnit);

		this.nodeApi = new NodeApi(
				nodeApiDeviceAdapter,
				(timeoutNodeAPI == null ? DEFAULT_NODE_API_TIMEOUT : timeoutNodeAPI),
				TimeUnit.MILLISECONDS
		);
		this.timeoutReset = timeoutReset == null ? 3000 : timeoutReset;
		this.timeoutFlash = timeoutFlash == null ? 90000 : timeoutFlash;

	}

	private boolean isConnected() {
		return deviceConnection != null && deviceConnection.isConnected();
	}

	@Override
	public void destroyVirtualLink(final long targetNode, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.destroyVirtualLink()", nodeUrn);

		if (isConnected()) {
			schedulerService.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getLinkControl().destroyVirtualLink(targetNode), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	private void callCallback(final Future<NodeApiCallResult> future, final Callback listener) {
		try {

			NodeApiCallResult result = future.get();

			if (result.isSuccessful()) {
				listener.success(result.getResponse());
			} else {
				listener.failure(result.getResponseType(), result.getResponse());
			}

		} catch (InterruptedException e) {
			log.error("InterruptedException while reading Node API call result.");
			listener.failure((byte) 127, "Unknown error in testbed back-end occurred!".getBytes());
		} catch (ExecutionException e) {
			if (e.getCause() instanceof TimeoutException) {
				log.debug("{} => Call to Node API timed out.", nodeUrn);
				listener.timeout();
			} else {
				log.error("" + e, e);
				listener.failure((byte) 127, "Unknown error in testbed back-end occurred!".getBytes());
			}
		}
	}

	@Override
	public void disableNode(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.disableNode()", nodeUrn);

		if (isConnected()) {
			schedulerService.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getNodeControl().disableNode(), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	@Override
	public void disablePhysicalLink(final long nodeB, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.disablePhysicalLink()", nodeUrn);

		if (isConnected()) {
			schedulerService.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getLinkControl().disablePhysicalLink(nodeB), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	@Override
	public void enableNode(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.enableNode()", nodeUrn);

		if (isConnected()) {
			schedulerService.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getNodeControl().enableNode(), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}
	}

	@Override
	public void enablePhysicalLink(final long nodeB, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.enablePhysicalLink()", nodeUrn);

		if (isConnected()) {
			schedulerService.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getLinkControl().enablePhysicalLink(nodeB), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}
	}

	private boolean isFlashOperationRunningOrEnqueued() {
		for (Operation<?> operation : deviceOperationQueue.getOperations()) {
			if (operation instanceof ProgramOperation) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void flashProgram(final WSNAppMessages.Program program,
							 final FlashProgramCallback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.executeFlashPrograms()", nodeUrn);

		if (isConnected()) {

			if (isFlashOperationRunningOrEnqueued()) {

				String msg = "There's a flash operation running or enqueued currently. Please try again later.";
				log.warn("{} => flashProgram: {}", nodeUrn, msg);
				listener.failure((byte) -1, msg.getBytes());

			} else {

				device.program(program.getProgram().toByteArray(), timeoutFlash, new AsyncAdapter<Void>() {

					private int lastProgress = -1;

					@Override
					public void onExecute() {
						flashCount = (flashCount % Integer.MAX_VALUE) == 0 ? 0 : flashCount++;
						lastProgress = 0;
						listener.progress(0f);
					}

					@Override
					public void onCancel() {
						String msg = "Flash operation was canceled.";
						log.error("{} => flashProgram: {}", nodeUrn, msg);
						listener.failure((byte) -1, msg.getBytes());
					}

					@Override
					public void onFailure(final Throwable throwable) {

						String msg;

						if (throwable instanceof ProgramChipMismatchException) {
							msg = "Error reading binary image: " + throwable;
						} else {
							msg = "Failed flashing node. Reason: " + throwable;
						}

						log.warn("{} => flashProgram: {}", nodeUrn, msg);
						listener.failure((byte) -3, msg.getBytes());
					}

					@Override
					public void onProgressChange(final float fraction) {

						int newProgress = (int) (fraction * 100);

						if (newProgress > lastProgress) {

							log.debug("{} => Flashing progress: {}%.", nodeUrn, newProgress);
							listener.progress(fraction);
							lastProgress = newProgress;
						}
					}

					@Override
					public void onSuccess(final Void result) {

						log.debug("{} => Done flashing node.", nodeUrn);
						listener.success(null);
					}
				}
				);

			}

		} else {
			String msg = "Failed flashing node. Reason: Node is not connected.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) -2, msg.getBytes());
		}

	}

	@Override
	public void isNodeAlive(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.isNodeAlive()", nodeUrn);

		// to the best of our knowledge, a node is alive if we're connected to it
		if (isConnected()) {
			listener.success(null);
		} else {
			listener.failure((byte) 0, "Device is not connected.".getBytes());
		}
	}

	@Override
	public void resetNode(final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.resetNode()", nodeUrn);

		if (isConnected()) {

			device.reset(timeoutReset, new AsyncAdapter<Void>() {

				@Override
				public void onExecute() {
					resetCount = (resetCount % Integer.MAX_VALUE) == 0 ? 0 : resetCount++;
				}

				@Override
				public void onSuccess(final Void result) {
					log.debug("{} => Done resetting node.", nodeUrn);
					listener.success(null);
				}

				@Override
				public void onCancel() {
					listener.failure((byte) -1, "Operation was cancelled.".getBytes());
				}

				@Override
				public void onFailure(final Throwable throwable) {
					String msg = "Failed resetting node. Reason: " + throwable;
					log.warn("{} => resetNode(): {}", nodeUrn, msg);
					listener.failure((byte) -1, msg.getBytes());
				}
			}
			);

		} else {

			String msg = "Failed resetting node. Reason: Device is not connected.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) 0, msg.getBytes());
		}

	}

	@Override
	public void sendMessage(final byte[] messageBytes, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.sendMessage()", nodeUrn);

		if (isConnected()) {

			if (isVirtualLinkMessage(messageBytes)) {

				log.debug("{} => Delivering virtual link message over node API", nodeUrn);

				ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);

				final byte RSSI = messageBuffer.get(3);
				final byte LQI = messageBuffer.get(4);
				final byte payloadLength = messageBuffer.get(5);
				final long destination = messageBuffer.getLong(6);
				final long source = messageBuffer.getLong(14);
				final byte[] payload = new byte[payloadLength];

				System.arraycopy(messageBytes, 22, payload, 0, payloadLength);

				schedulerService.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(
								nodeApi.getInteraction().sendVirtualLinkMessage(RSSI, LQI, destination, source,
										payload
								),
								listener
						);
					}
				}
				);

			} else {

				log.debug("{} => Delivering message directly over iSenseDevice.send(), i.e. not as a virtual link "
						+ "message.", nodeUrn
				);

				int packetType = messageBytes[0] & 0xFF;
				byte[] packetBytes = new byte[messageBytes.length - 1];
				System.arraycopy(messageBytes, 0, packetBytes, 0, packetBytes.length);

				device.send(new MessagePacket(packetType, packetBytes), 1000, new AsyncAdapter<Void>() {
					@Override
					public void onSuccess(final Void result) {
						listener.success(null);
					}

					@Override
					public void onFailure(final Throwable throwable) {
						String msg = "Failed sending message. Reason: " + throwable;
						log.warn("{} => sendMessage(): {}", nodeUrn, msg);
						listener.failure((byte) -2, msg.getBytes());
					}

					@Override
					public void onCancel() {
						String msg = "Sending message was canceled.";
						log.warn("{} => sendMessage(): {}", nodeUrn, msg);
						listener.failure((byte) -3, msg.getBytes());
					}
				}
				);

			}

		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}

	}

	private boolean isVirtualLinkMessage(final byte[] messageBytes) {
		return messageBytes.length > 1 && messageBytes[0] == MESSAGE_TYPE_WISELIB_DOWNSTREAM && messageBytes[1] == VIRTUAL_LINK_MESSAGE;
	}

	@Override
	public void setVirtualLink(final long targetNode, final Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.setVirtualLink()", nodeUrn);

		if (isConnected()) {
			schedulerService.execute(new Runnable() {
				@Override
				public void run() {
					callCallback(nodeApi.getLinkControl().setVirtualLink(targetNode), listener);
				}
			}
			);
		} else {
			listener.failure((byte) -1, "Node is not connected.".getBytes());
		}
	}

	@Override
	public void start() throws Exception {
		schedulerService.schedule(connectRunnable, 0, TimeUnit.MILLISECONDS);
		nodeApi.start();
	}

	@Override
	public void stop() {

		nodeApi.stop();

		if (device != null) {
			device.removeListener(messagePacketListener);
			log.debug("{} => Shutting down {} device", nodeUrn, nodeType);
			deviceConnection.shutdown(true);
		}

	}
}
