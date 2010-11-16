package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.gtr.common.AbstractListenable;
import de.uniluebeck.itm.gtr.common.SchedulerService;
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

		private final TimeDiff lastProgress = new TimeDiff(1500);

		private FlashProgramCallback listener;

		private FlashProgramListener(final FlashProgramCallback listener) {
			this.listener = listener;
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

			if (op == Operation.PROGRAM) {

				listener.failure((byte) -1, "Operation was cancelled.".getBytes());
				device.deregisterListener(FlashProgramListener.this);
				state.setState(State.READY);

			} else {
				log.error(
						"Received operation state from device drivers != PROGRAM ({}) which should not be possible",
						op
				);
			}
		}

		@Override
		public void operationDone(final Operation op, final Object result) {

			if (op == Operation.PROGRAM) {

				if (result instanceof Exception) {
					log.debug("{} => Failed flashing node. Reason: {}", nodeUrn, result);
					listener.failure((byte) -1, ((Exception) result).getMessage().getBytes());
				} else {
					log.debug("{} => Done flashing node.", nodeUrn, result);
					listener.success(null);
				}

				log.debug("{} => Setting state to READY and deregistering FlashProgramListener instance.", nodeUrn);
				device.deregisterListener(this);
				state.setState(State.READY);

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
					log.debug("{} => Flashing progress: {}%.", (int) (fraction * 100));
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

	}

	private class ResetListener implements iSenseDeviceListener, Comparable<iSenseDeviceListener> {

		private NodeApiCallback listener;

		private ResetListener(NodeApiCallback listener) {
			this.listener = listener;
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

			if (op == Operation.RESET) {

				listener.failure((byte) -1, "Operation was cancelled.".getBytes());
				device.deregisterListener(this);
				state.setState(State.READY);

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

			if (op == Operation.RESET) {

				if (result instanceof Exception) {
					log.debug("{} => Failed resetting node. Reason: {}", nodeUrn, result);
					listener.failure((byte) -1, ((Exception) result).getMessage().getBytes());
				} else if (result instanceof Boolean && ((Boolean) result)) {
					log.debug("{} => Done resetting node.", nodeUrn);
					listener.success(null);
				} else {
					log.debug("{} => Failed resetting node.");
					listener.failure((byte) -1, "Could not reset node.".getBytes());
				}

				log.debug("{} => Setting state to READY and deregistering ResetListener instance.", nodeUrn);
				device.deregisterListener(this);
				state.setState(State.READY);

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

	}

	private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

	private static final byte NODE_OUTPUT_TEXT = 50;

	private static final byte NODE_OUTPUT_BYTE = 51;

	private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

	private static final byte VIRTUAL_LINK_MESSAGE = 11;

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceAppConnector.class);

	private static final int DEFAULT_NODE_API_TIMEOUT = 5000;

	private String nodeType;

	private String nodeUSBChipID;

	private String nodeSerialInterface;

	private SchedulerService schedulerService;

	private ConnectorState state = new ConnectorState();

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
									  final String nodeSerialInterface, final Integer nodeAPITimeout,
									  final SchedulerService schedulerService) {

		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeUSBChipID = nodeUSBChipID;
		this.nodeSerialInterface = nodeSerialInterface;

		this.schedulerService = schedulerService;

		this.nodeApi =
				new NodeApi(nodeApiDeviceAdapter, nodeAPITimeout == null ? DEFAULT_NODE_API_TIMEOUT : nodeAPITimeout,
						TimeUnit.MILLISECONDS
				);
	}

	@Override
	public void destroyVirtualLink(final long targetNode, final NodeApiCallback listener) {
		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				nodeApi.getLinkControl().destroyVirtualLink(targetNode, listener);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void disableNode(final NodeApiCallback listener) {
		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				nodeApi.getNodeControl().disableNode(listener);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void disablePhysicalLink(final long nodeB, final NodeApiCallback listener) {
		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				nodeApi.getLinkControl().disablePhysicalLink(nodeB, listener);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void enableNode(final NodeApiCallback listener) {
		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				nodeApi.getNodeControl().enableNode(listener);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void enablePhysicalLink(final long nodeB, final NodeApiCallback listener) {
		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				nodeApi.getLinkControl().enablePhysicalLink(nodeB, listener);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void flashProgram(final WSNAppMessages.Program program, final FlashProgramCallback listener) {

		log.debug("{} => WSNDeviceAppImpl.executeFlashPrograms()", nodeUrn);

		if (state.getState() == State.DISCONNECTED || !device.isConnected()) {
			String msg = "Failed flashing node. Reason: Node is not connected.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) -1, msg.getBytes());
			return;
		}

		try {
			state.setState(State.OPERATION_RUNNING);
		} catch (RuntimeException e) {
			String msg = "There's an operation (flashProgram, resetNode) running currently. Please try again later.";
			log.warn("{} => {}", nodeUrn, msg);
			listener.failure((byte) -1, msg.getBytes());
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
			log.error("{} => {}", nodeUrn, msg);
			listener.failure((byte) -1, msg.getBytes());
			state.setState(State.READY);
			return;
		}

		FlashProgramListener flashListener = new FlashProgramListener(listener);
		device.registerListener(flashListener);

		try {

			if (!device.triggerProgram(iSenseBinFile, true)) {
				listener.failure((byte) 0, "Failed to trigger programming.".getBytes());
				device.deregisterListener(flashListener);
				state.setState(State.READY);
			}

		} catch (Exception e) {
			log.error("{} => Error while flashing device. Reason: {}", nodeUrn, e.getMessage());
			listener.failure((byte) 0, ("Error while flashing device. Reason: " + e.getMessage()).getBytes());
			device.deregisterListener(flashListener);
			state.setState(State.READY);
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
			listener.failure((byte) 0, "Device is not connected.".getBytes());
		}
	}

	@Override
	public void resetNode(final NodeApiCallback listener) {

		log.debug("{} => WSNDeviceAppImpl.executeResetNodes()", nodeUrn);

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
		}

		ResetListener resetListener = new ResetListener(listener);
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
	public void sendMessage(final byte messageType, final byte[] messageBytes, final NodeApiCallback listener) {

		switch (state.getState()) {

			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;

			case READY:
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

						nodeApi.getInteraction()
								.sendVirtualLinkMessage(RSSI, LQI, destination, source, payload, listener);

					} else {

						log.debug(
								"{} => Delivering message directly over iSenseDevice.send(), i.e. not as a virtual link message.",
								nodeUrn
						);

						device.send(new MessagePacket(messageType, messageBytes));
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

	@Override
	public void setVirtualLink(final long targetNode, final NodeApiCallback listener) {
		switch (state.getState()) {
			case DISCONNECTED:
				listener.failure((byte) -1, "Node is not connected.".getBytes());
				break;
			case READY:
				nodeApi.getLinkControl().setVirtualLink(targetNode, listener);
				break;
			case OPERATION_RUNNING:
				listener.failure((byte) -1, "Node is currently being reprogrammed/reset. Try again later.".getBytes());
				break;
		}
	}

	@Override
	public void start() throws Exception {
		schedulerService.schedule(connectRunnable, 0, TimeUnit.MILLISECONDS);
	}

	@Override
	public void stop() {

		if (device != null) {
			device.deregisterListener(deviceOutputListener);
			log.debug("{} => Shutting down {} device", nodeUrn, nodeType);
			device.shutdown();
		}

	}
}
