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
import de.uniluebeck.itm.tr.nodeapi.NodeApi;
import de.uniluebeck.itm.tr.nodeapi.NodeApiCallResult;
import de.uniluebeck.itm.tr.nodeapi.NodeApiDeviceAdapter;
import de.uniluebeck.itm.tr.util.*;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.TimeoutException;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class WSNDeviceAppConnectorLocal extends AbstractListenable<WSNDeviceAppConnector.NodeOutputListener>
		implements WSNDeviceAppConnector {

	enum State {
		DISCONNECTED, READY, OPERATION_RUNNING
	}

	private static class ConnectorState {

		private State state;

		private final ReentrantLock stateLock = new ReentrantLock();

		private ConnectorState() {
			state = State.DISCONNECTED;
		}

		public void setState(State newState) {
			try {
				stateLock.lock();
				if (state == State.OPERATION_RUNNING && newState == State.OPERATION_RUNNING) {
					throw new RuntimeException("There's another operation currently running.");
				}
				state = newState;
			} finally {
				stateLock.unlock();
			}
		}

		public State getState() {
			try {
				stateLock.lock();
				return state;
			} finally {
				stateLock.unlock();
			}
		}

	}

	private class FlashProgramListener implements iSenseDeviceListener, Comparable<iSenseDeviceListener> {

		private int flashCount;

		private final ScheduledFuture<?> flashTimeoutRunnable;

		private final TimeDiff lastProgress = new TimeDiff(3000);

		private FlashProgramCallback listener;

		private FlashProgramListener(final FlashProgramCallback listener, int flashCount,
									 final ScheduledFuture<?> flashTimeoutRunnable) {
			this.listener = listener;
			this.flashCount = flashCount;
			this.flashTimeoutRunnable = flashTimeoutRunnable;
		}

		@Override
		public void receivePacket(final MessagePacket p) {
			// nothing to do
		}

		@Override
		public void receivePlainText(final MessagePlainText p) {
			// nothing to do
		}

		@Override
		public void operationCanceled(final Operation op) {

			log.debug("{} => WSNDeviceAppConnectorLocal.FlashProgramListener.operationCancelled({})", nodeUrn, op);

			if (op == Operation.PROGRAM) {

				try {
					log.warn("{} => Operation was cancelled: {}", nodeUrn, op);

					flashTimeoutRunnable.cancel(true);
					listener.failure((byte) -1, "Operation was cancelled.".getBytes());
				} finally {
					log.debug("{} => Setting state to READY and de-registering FlashProgramListener instance.", nodeUrn
					);
					device.deregisterListener(FlashProgramListener.this);
					state.setState(State.READY);
				}

			} else {
				log.error(
						"{} => Received operation state from device drivers != PROGRAM ({}) which should not be possible",
						nodeUrn,
						op
				);
			}
		}

		@Override
		public void operationDone(final Operation op, final Object result) {

			log.debug("{} => WSNDeviceAppConnectorLocal.FlashProgramListener.operationDone({})", nodeUrn, op);

			if (op == Operation.PROGRAM) {

				try {
					flashTimeoutRunnable.cancel(true);

					if (result instanceof Exception) {
						log.debug("{} => Failed flashing node. Reason: {}", nodeUrn, result);
						String message = "Failed flashing node. Reason: " + result;
						listener.failure((byte) -1, message.getBytes());
					} else {
						log.debug("{} => Done flashing node.", nodeUrn, result);
						listener.success(null);
					}

				} finally {
					log.debug("{} => Setting state to READY and de-registering FlashProgramListener instance.", nodeUrn
					);
					device.deregisterListener(this);
					state.setState(State.READY);
				}

			} else {
				log.error(
						"{} => Received operation state from device drivers != PROGRAM ({}) which should not be possible",
						nodeUrn,
						op
				);
			}
		}

		@Override
		public void operationProgress(final Operation op, final float fraction) {

			if (op == Operation.PROGRAM) {

				if (lastProgress.isTimeout()) {
					log.debug("{} => Flashing progress: {}%.", nodeUrn, (int) (fraction * 100));
					listener.progress(fraction);
					lastProgress.touch();
				}

			} else {
				log.error(
						"{} => Received operation state from device drivers != PROGRAM ({}) which should not be possible",
						nodeUrn,
						op
				);
			}
		}

		@Override
		public int compareTo(final iSenseDeviceListener o) {
			return o == this ? 0 : -1;
		}

		@Override
		public String toString() {
			return "FlashProgramListener{" +
					"flashCount=" + flashCount +
					'}';
		}
	}

	private class ResetListener implements iSenseDeviceListener, Comparable<iSenseDeviceListener> {

		private int resetCount;

		private final ScheduledFuture<?> resetTimeoutRunnableFuture;

		private Callback listener;

		private ResetListener(Callback listener, int resetCount, final ScheduledFuture<?> resetTimeoutRunnableFuture) {
			this.listener = listener;
			this.resetCount = resetCount;
			this.resetTimeoutRunnableFuture = resetTimeoutRunnableFuture;
		}

		@Override
		public void receivePacket(final MessagePacket p) {
			// nothing to do
		}

		@Override
		public void receivePlainText(final MessagePlainText p) {
			// nothing to do
		}

		@Override
		public void operationCanceled(final Operation op) {

			log.debug("{} => WSNDeviceAppConnectorLocal.ResetListener.operationCanceled({})", nodeUrn, op);

			if (op == Operation.RESET) {

				try {
					resetTimeoutRunnableFuture.cancel(true);

					listener.failure((byte) -1, "Operation was cancelled.".getBytes());
				} finally {
					log.debug("{} => Setting state to READY and de-registering ResetListener instance.", nodeUrn);
					device.deregisterListener(this);
					state.setState(State.READY);
				}

			} else {
				log.error(
						"{} => Received operation state from device drivers != RESET ({}) which should not be possible",
						nodeUrn,
						op
				);
			}

		}

		@Override
		public void operationDone(final Operation op, final Object result) {

			if (log.isDebugEnabled()) {
				log.debug("{} => WSNDeviceAppConnectorLocal.ResetListener.operationDone({}, {})",
						new Object[]{nodeUrn, op, result}
				);
			}

			if (op == Operation.RESET) {

				try {
					resetTimeoutRunnableFuture.cancel(true);

					if (result instanceof Exception) {
						log.debug("{} => Failed resetting node. Reason: {}", nodeUrn, result);
						String msg = "Failed resetting node. Reason: " + result;
						listener.failure((byte) -1, msg.getBytes());
					} else if (result instanceof Boolean && ((Boolean) result)) {
						log.debug("{} => Done resetting node.", nodeUrn);
						listener.success(null);
					} else {
						log.debug("{} => Failed resetting node.");
						listener.failure((byte) -1, "Could not reset node.".getBytes());
					}
				} finally {
					log.debug("{} => Setting state to READY and de-registering ResetListener instance.", nodeUrn);
					device.deregisterListener(this);
					state.setState(State.READY);
				}


			} else {
				log.error(
						"{} => Received operation state from device drivers != RESET ({}) which should not be possible",
						nodeUrn,
						op
				);
			}
		}

		@Override
		public void operationProgress(final Operation op, final float fraction) {
			// nothing to do
		}

		@Override
		public int compareTo(final iSenseDeviceListener o) {
			return o == this ? 0 : -1;
		}

		@Override
		public String toString() {
			return "ResetListener{" +
					"resetCount=" + resetCount +
					'}';
		}

	}

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

	private final ConnectorState state = new ConnectorState();

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

			try {

				device = DeviceFactory.create(nodeType, nodeSerialInterface);
				device.setLogIdentifier(nodeUrn + " => ");

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

			// attach as listener to device output
			device.registerListener(deviceOutputListener);
			state.setState(State.READY);

		}
	};

	/**
	 * Listener that grabs all sensor node outputs and forwards them to interest listeners. Other device listeners for
	 * flashing and resetting operations are registered as needed and immediately removed after completion of the
	 * operation.
	 */
	private iSenseDeviceListener deviceOutputListener = new iSenseDeviceListenerAdapter() {

		@Override
		public void receivePacket(MessagePacket p) {
			//if reaching maximum-message-rate do not send more then 1 message
			if (!maximumMessageRateLimiter.checkIfInSlotAndCount()) {
				int dismissedCount = maximumMessageRateLimiter.dismissedCount();
				if (dismissedCount >= 1) {
					sendPacketsDroppedWarningIfNotificationRateAllows(dismissedCount);
				}
				return;
			}

			log.trace("{} => WSNDeviceAppConnectorLocal.receivePacket: {}", nodeUrn, p);

			boolean isWiselibUpstream = p.getType() == MESSAGE_TYPE_WISELIB_UPSTREAM;
			boolean isByteTextOrVLink =
					(p.getContent()[0] & 0xFF) == NODE_OUTPUT_BYTE ||
							(p.getContent()[0] & 0xFF) == NODE_OUTPUT_TEXT ||
							(p.getContent()[0] & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK;

			boolean isWiselibReply = isWiselibUpstream && !isByteTextOrVLink;

			if (isWiselibReply && nodeApiDeviceAdapter.receiveFromNode(ByteBuffer.wrap(p.getContent()))) {
				if (log.isDebugEnabled()) {
					log.debug("{} => Received WISELIB_UPSTREAM packet with content: {}", nodeUrn, p);
				}
			} else {
				for (NodeOutputListener listener : listeners) {
					listener.receivedPacket(p);
				}
			}

		}

		@Override
		public void operationCanceled(Operation operation) {
			// nothing to do
		}

		@Override
		public void operationDone(Operation operation, Object o) {
			// nothing to do
		}

		@Override
		public void operationProgress(Operation operation, float v) {
			// nothing to do
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
				device.send(new MessagePacket(MESSAGE_TYPE_WISELIB_DOWNSTREAM, packet.array()));
			} catch (Exception e) {
				log.error("" + e, e);
			}
		}
	};

	private String nodeUrn;

	private iSenseDevice device;

	public WSNDeviceAppConnectorLocal(final String nodeUrn, final String nodeType, final String nodeUSBChipID,
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

	@Override
	public void destroyVirtualLink(final long targetNode, final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.destroyVirtualLink()", nodeUrn);

		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				schedulerService.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(nodeApi.getLinkControl().destroyVirtualLink(targetNode), listener);
					}
				}
				);

				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	private void callCallback(final Future<NodeApiCallResult> future, final WSNDeviceAppConnector.Callback listener) {
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
	public void disableNode(final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.disableNode()", nodeUrn);

		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				schedulerService.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(nodeApi.getNodeControl().disableNode(), listener);
					}
				}
				);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void disablePhysicalLink(final long nodeB, final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.disablePhysicalLink()", nodeUrn);

		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				schedulerService.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(nodeApi.getLinkControl().disablePhysicalLink(nodeB), listener);
					}
				}
				);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void enableNode(final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.enableNode()", nodeUrn);

		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				schedulerService.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(nodeApi.getNodeControl().enableNode(), listener);
					}
				}
				);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void enablePhysicalLink(final long nodeB, final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.enablePhysicalLink()", nodeUrn);

		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				schedulerService.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(nodeApi.getLinkControl().enablePhysicalLink(nodeB), listener);
					}
				}
				);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void flashProgram(final WSNAppMessages.Program program,
							 final WSNDeviceAppConnector.FlashProgramCallback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.executeFlashPrograms()", nodeUrn);

		if (state.getState() == State.DISCONNECTED || !device.isConnected()) {
			String msg = "Failed flashing node. Reason: Node is not connected.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) -2, msg.getBytes());
			return;
		}

		try {
			state.setState(State.OPERATION_RUNNING);
		} catch (RuntimeException e) {
			String msg = "There's an operation (flashProgram, resetNode) running currently. Please try again later.";
			log.warn("{} => flashProgram: {}", nodeUrn, msg);
			listener.failure((byte) -1, msg.getBytes());
			return;
		}

		IDeviceBinFile iSenseBinFile = null;

		try {

			if (device instanceof JennicDevice) {
				iSenseBinFile =
						new JennicBinFile(program.getProgram().toByteArray(), program.getMetaData().toString());
			} else if (device instanceof TelosbDevice) {
				iSenseBinFile =
						new TelosbBinFile(program.getProgram().toByteArray(), program.getMetaData().toString());
			} else if (device instanceof PacemateDevice) {
				iSenseBinFile =
						new PacemateBinFile(program.getProgram().toByteArray(), program.getMetaData().toString());
			}

		} catch (Exception e) {
			String msg = "Error reading bin file. Reason: " + e;
			log.error("{} => flashProgram: {}", nodeUrn, msg);
			listener.failure((byte) -3, msg.getBytes());
			state.setState(State.READY);
			return;
		}

		flashCount = (flashCount % Integer.MAX_VALUE) == 0 ? 0 : flashCount++;

		// bugfix for:
		// https://www.itm.uni-luebeck.de/projects/testbed-runtime/ticket/14
		// https://www.itm.uni-luebeck.de/projects/testbed-runtime/ticket/134

		// it seems the driver "hangs up" sometimes so the listener doesn't get called. to make sure we can go on
		// with our work whatsoever schedule a task that the state is set back from OPERATION_RUNNING to READY
		// and to cancel running operations if there are any

		// this task should only execute if the flashListener was not called through the device driver
		ScheduledFuture<?> flashTimeoutRunnable = schedulerService.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					device.cancelOperation(Operation.WRITE_FLASH);
				} catch (Exception e) {
					log.warn("" + e, e);
				}
				state.setState(State.READY);
			}
		}, timeoutFlash, TimeUnit.MILLISECONDS
		);
		// end bugfix

		FlashProgramListener flashListener = new FlashProgramListener(listener, flashCount, flashTimeoutRunnable);
		device.registerListener(flashListener);

		try {

			if (!device.triggerProgram(iSenseBinFile, true)) {
				String msg = "Failed to trigger programming.";
				log.debug("{} => flashProgram: {}", nodeUrn, msg);
				listener.failure((byte) -4, msg.getBytes());
				device.deregisterListener(flashListener);
				state.setState(State.READY);
			}

		} catch (Exception e) {
			log.error("{} => flashProgram: Error while flashing device. Reason: {}", nodeUrn, e.getMessage());
			listener.failure((byte) -5, ("Error while flashing device. Reason: " + e.getMessage()).getBytes());
			device.deregisterListener(flashListener);
			state.setState(State.READY);
		}

	}

	@Override
	public void isNodeAlive(final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.isNodeAlive()", nodeUrn);

		// to the best of our knowledge, a node is alive if we're connected to it
		boolean connected = device != null && device.isConnected();
		if (connected) {
			listener.success(null);
		} else {
			listener.failure((byte) 0, "Device is not connected.".getBytes());
		}
	}

	@Override
	public void resetNode(final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.resetNode()", nodeUrn);

		if (state.getState() == State.DISCONNECTED || !device.isConnected()) {
			String msg = "Failed resetting node. Reason: Device is not connected.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) 0, msg.getBytes());
			return;
		}

		try {
			state.setState(State.OPERATION_RUNNING);
		} catch (RuntimeException e) {
			String msg = "There's an operation (flashProgram or resetNode) running currently. Please try again later.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) 0, msg.getBytes());
			return;
		}

		resetCount = (resetCount % Integer.MAX_VALUE) == 0 ? 0 : resetCount++;

		ScheduledFuture<?> resetTimeoutRunnableFuture = schedulerService.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					device.cancelOperation(Operation.RESET);
				} catch (Exception e) {
					log.warn("" + e, e);
				}
				state.setState(State.READY);
			}
		}, timeoutReset, TimeUnit.MILLISECONDS
		);

		ResetListener resetListener = new ResetListener(listener, resetCount, resetTimeoutRunnableFuture);
		device.registerListener(resetListener);

		try {

			boolean triggered = device.triggerReboot();
			if (!triggered) {
				String msg = "Failed resetting node. Reason: Could not trigger reboot.";
				log.warn("{} => {}", nodeUrn, msg);
				listener.failure((byte) 0, msg.getBytes());
				device.deregisterListener(resetListener);
				state.setState(State.READY);
			}

		} catch (Exception e) {
			// caught from device.triggerReboot()
			String msg = "Error while resetting device: " + e.getMessage();
			log.error("{} => {}", nodeUrn, msg);
			listener.failure((byte) 0, msg.getBytes());
			device.deregisterListener(resetListener);
			state.setState(State.READY);
		}

	}

	@Override
	public void sendMessage(final byte[] messageBytes, final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.sendMessage()", nodeUrn);

		switch (state.getState()) {

			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;

			case READY:
				try {

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

						if (log.isDebugEnabled()) {
							log.debug(
									"{} => Delivering non-virtual link message: {}",
									nodeUrn,
									StringUtils.toHexString(messageBytes)
							);
						}

						device.send(new MessagePacket(messageBytes));
						listener.success(null);

					}

				} catch (Exception e) {
					log.error("" + e, e);
					listener.failure((byte) -1, e.getMessage().getBytes());
				}
				break;

			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;

		}

	}

	private boolean isVirtualLinkMessage(final byte[] messageBytes) {
		return messageBytes.length > 1 && messageBytes[0] == MESSAGE_TYPE_WISELIB_DOWNSTREAM && messageBytes[1] == VIRTUAL_LINK_MESSAGE;
	}

	@Override
	public void setVirtualLink(final long targetNode, final WSNDeviceAppConnector.Callback listener) {

		log.debug("{} => WSNDeviceAppConnectorLocal.setVirtualLink()", nodeUrn);

		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				schedulerService.execute(new Runnable() {
					@Override
					public void run() {
						callCallback(nodeApi.getLinkControl().setVirtualLink(targetNode), listener);
					}
				}
				);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
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
			device.deregisterListener(deviceOutputListener);
			log.debug("{} => Shutting down {} device", nodeUrn, nodeType);
			device.shutdown();
		}

	}
}
