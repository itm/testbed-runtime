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

package de.uniluebeck.itm.wsn.devicedrivers.mockdevice;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.FlashType;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.Sectors;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.sleep;

/**
 * A mock device that is "always working" and emits "alive"-messages at a configurable rate.
 *
 * @author Daniel Bimschas
 */
public class MockDevice extends iSenseDeviceImpl {

	public static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	public static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

	public static final byte MESSAGE_TYPE_MOCK_DEVICE_PING = (byte) 0xFF;

	public static final byte NODE_API_VL_MESSAGE = 11;

	/**
	 *
	 */
	private String nodeName;

	private List<Long> virtualLinks = new ArrayList<Long>();

	private long nodeId;

	private ScheduledFuture<?> virtualLinkRunnableFuture;

	private class AliveRunnable implements Runnable {

		private int i = 0;

		private DateTime started;

		private AliveRunnable() {
			started = new DateTime();
		}

		@Override
		public void run() {
			DateTime now = new DateTime();
			Interval interval = new Interval(started, now);
			String msg = "MockDevice " + nodeName + " alive since " + interval.toDuration().getStandardSeconds() +
					" seconds (update #" + (++i) + ")";
			sendLogMessage(msg);
			sendBinaryMessage((byte) 0x00, msg.getBytes());
		}

	}

	private class VirtualLinkRunnable implements Runnable {

		private int i = 0;

		private DateTime started;

		private VirtualLinkRunnable() {
			started = new DateTime();
		}

		@Override
		public void run() {
			DateTime now = new DateTime();
			Interval interval = new Interval(started, now);
			sendVirtualLinkMessage("MockDevice " + nodeName + " alive since " + interval.toDuration()
					.getStandardSeconds() + " seconds (update #" + (++i) + ")"
			);
		}

	}

	/**
	 *
	 */
	private Future<?> aliveRunnableFuture;

	/**
	 *
	 */
	private long aliveTimeout;

	/**
	 *
	 */
	private TimeUnit aliveTimeUnit;

	/**
	 *
	 */
	private boolean rebootAfterFlashing;

	/**
	 *
	 */
	private ScheduledExecutorService executorService =
			Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder().setNameFormat("MockDevice-Thread %d").build());

	/**
	 * Instantiates a new mock device with the given configuration.
	 *
	 * @param config a comma-separated String containing an int defining the timeout and a serialized {@link
	 *               java.util.concurrent.TimeUnit} defining the time unit that is to be used for the timeout. The timeout
	 *               value defines how often the mock device sends "alive"-messages.
	 */
	public MockDevice(String config) {

		checkNotNull(config);

		String[] strs = config.split(",");
		checkArgument(strs.length == 3, "The port (serialinterface) value must contain three comma-separated values: "
				+ "nodeName,aliveMessageTimeout,aliveMessageTimeUnit where aliveMessageTimeUnit is one of "
				+ "NANOSECONDS,MICROSECONDS,MILLISECONDS,SECONDS,MINUTES,HOURS,DAYS."
		);

		this.nodeName = strs[0];
		checkArgument(!"".equals(nodeName), "The value nodeName must not be empty!");

		String[] nodeNameParts = nodeName.split(":");
		try {
			this.nodeId = StringUtils.parseHexOrDecLong(nodeNameParts[nodeNameParts.length - 1]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					"The last part of the node URN must be a long value. Failed to parse it..."
			);
		}

		this.aliveTimeout = Long.parseLong(strs[1]);
		checkArgument(aliveTimeout > 0, "The value aliveMessageTimeout must be greater than zero (0)!");

		this.aliveTimeUnit = TimeUnit.valueOf(strs[2]);

		scheduleAliveRunnable();
		scheduleVirtualLinkRunnable();

	}

	private void scheduleAliveRunnable() {
		this.aliveRunnableFuture = this.executorService
				.scheduleWithFixedDelay(new AliveRunnable(), new Random().nextInt((int) aliveTimeout), aliveTimeout,
						aliveTimeUnit
				);
	}

	private void scheduleVirtualLinkRunnable() {
		this.virtualLinkRunnableFuture = this.executorService
				.scheduleWithFixedDelay(new VirtualLinkRunnable(), new Random().nextInt((int) aliveTimeout),
						aliveTimeout,
						aliveTimeUnit
				);
	}

	private void stopAliveRunnable() {
		if (aliveRunnableFuture != null && !aliveRunnableFuture.isCancelled()) {
			aliveRunnableFuture.cancel(true);
		}
	}

	private void stopVirtualLinkRunnable() {
		if (virtualLinkRunnableFuture != null && !virtualLinkRunnableFuture.isCancelled()) {
			virtualLinkRunnableFuture.cancel(true);
		}
	}

	@Override
	public boolean enterProgrammingMode() throws Exception {
		return true;
	}

	@Override
	public void eraseFlash() throws Exception {
		// nothing to do
	}

	@Override
	public ChipType getChipType() throws Exception {
		return ChipType.Unknown;
	}

	@Override
	public FlashType getFlashType() throws Exception {
		return FlashType.Unknown;
	}

	@Override
	public Operation getOperation() {
		if (operation == null) {
			return Operation.NONE;
		} else {
			return operation.getOperation();
		}
	}

	@Override
	public void leaveProgrammingMode() throws Exception {
		// nothing to do
	}

	@Override
	public byte[] readFlash(int address, int len) throws Exception {
		return new byte[]{};
	}

	private class ResetRunnable implements Runnable {

		@Override
		public void run() {
			try {
				sleep(200);
				stopAliveRunnable();
				stopVirtualLinkRunnable();
				sleep(1000);
				sendLogMessage("Booting MockDevice...");
				sleep(100);
				scheduleAliveRunnable();
				scheduleVirtualLinkRunnable();
			} catch (InterruptedException e) {
				logError("" + e, e);
			}

		}
	}

	@Override
	public boolean reset() throws Exception {
		executorService.submit(new ResetRunnable());
		return true;
	}

	public final static byte NODE_API_SET_VIRTUAL_LINK = 30;

	public final static byte NODE_API_DESTROY_VIRTUAL_LINK = 31;

	@Override
	public void send(MessagePacket p) throws Exception {

		// simulate WISELIB behaviour
		if (p.getType() == MESSAGE_TYPE_WISELIB_DOWNSTREAM) {

			logDebug("Received WISELIB downstream message: {}", p);

			byte[] payload = p.getContent();
			ByteBuffer payloadBuff = ByteBuffer.wrap(p.getContent());

			if (payload.length >= 2) {

				byte messageType = payloadBuff.get(0);
				byte requestId = payloadBuff.get(1);
				long destinationNode = payloadBuff.getLong(2);

				byte[] replyArr = new byte[3];

				if (messageType == NODE_API_DESTROY_VIRTUAL_LINK || messageType == NODE_API_SET_VIRTUAL_LINK || messageType == NODE_API_VL_MESSAGE) {

					if (messageType == NODE_API_SET_VIRTUAL_LINK) {

						replyArr[0] = NODE_API_SET_VIRTUAL_LINK;
						logDebug("Adding virtual link to node ID {}", destinationNode);
						virtualLinks.add(destinationNode);

					} else if (messageType == NODE_API_DESTROY_VIRTUAL_LINK) {

						replyArr[0] = NODE_API_DESTROY_VIRTUAL_LINK;
						logDebug("Removing virtual link to node ID {}", destinationNode);
						virtualLinks.remove(destinationNode);

					} else {

						logDebug("!!! Received virtual link message: {}", p);

					}

					replyArr[1] = requestId;
					replyArr[2] = 0;
					MessagePacket reply = new MessagePacket(MESSAGE_TYPE_WISELIB_UPSTREAM, replyArr);
					logDebug("Replying with WISELIB upstream packet: {}", reply);
					notifyReceivePacket(reply);

				}
			}
		} else if (p.getType() == MESSAGE_TYPE_MOCK_DEVICE_PING) {

			sendBinaryMessage(MESSAGE_TYPE_MOCK_DEVICE_PING, p.getContent());

		}
	}

	@Override
	public void eraseFlash(Sectors.SectorIndex sector) throws Exception {
		// nothing to do
	}

	@Override
	public void shutdown() {
        aliveRunnableFuture.cancel(true);
    }

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public void triggerGetMacAddress(boolean rebootAfterFlashing) throws Exception {
		// nothing to do
	}

	private class ProgramRunnable implements Runnable {

		@Override
		public void run() {

			Random rand = new Random();

			for (int i = 0; i < 100; i += rand.nextInt(10)) {

				try {
					sleep(1000);
				} catch (InterruptedException e) {
					logError("" + e, e);
				}
				MockDevice.this.operationProgress(Operation.PROGRAM, (float) i / (float) 100);

			}

			MockDevice.this.operationDone(Operation.PROGRAM, null);

			if (MockDevice.this.rebootAfterFlashing) {
				logDebug("Rebooting device");
				try {
					MockDevice.this.reset();
				} catch (Exception e) {
					logError("" + e, e);
				}
			}

			operation = null;
		}
	}

	@Override
	public boolean triggerProgram(IDeviceBinFile program, boolean rebootAfterFlashing) throws Exception {
		this.rebootAfterFlashing = rebootAfterFlashing;
		if (operationInProgress()) {
			logError("Already another operation in progress (" + operation + ")");
			return false;
		}
		executorService.execute(new ProgramRunnable());
		return true;
	}

	@Override
	public void triggerSetMacAddress(MacAddress mac, boolean rebootAfterFlashing) throws Exception {
		// nothing to do
	}

	@Override
	public byte[] writeFlash(int address, byte[] bytes, int offset, int len) throws Exception {
		return bytes;
	}

	private class RebootRunnable implements Runnable {

		@Override
		public void run() {
			executorService.execute(new ResetRunnable());
			MockDevice.this.operationDone(Operation.RESET, null);
		}
	}

	@Override
	public boolean triggerReboot() throws Exception {
		executorService.execute(new RebootRunnable());
		return true;
	}

	@Override
	public String toString() {
		return "MockDevice [" + nodeName + "]";
	}

	@Override
	public int[] getChannels() {
		return new int[]{11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
	}

	@Override
	public IDeviceBinFile loadBinFile(String fileName) {
		return new MockBinFile();
	}

	private void sendVirtualLinkMessage(final String message) {

		byte[] payload = message.getBytes();

		for (Long virtualLinkDestinationNode : virtualLinks) {

			logDebug("Sending virtual link message to node ID {}", virtualLinkDestinationNode);

			ByteBuffer bb = ByteBuffer.allocate(payload.length + 21);
			bb.put((byte) 52);
			bb.put((byte) 0);
			bb.put((byte) 0);
			bb.put((byte) payload.length);
			bb.putLong(virtualLinkDestinationNode);
			bb.putLong(nodeId);
			bb.put(payload);

			MessagePacket p = new MessagePacket(MESSAGE_TYPE_WISELIB_UPSTREAM, bb.array());
			notifyReceivePacket(p);

		}

	}

	private void sendLogMessage(String message) {

		byte[] msgBytes = message.getBytes();
		byte[] bytes = new byte[msgBytes.length + 2];
		bytes[0] = PacketTypes.LOG;
		bytes[1] = PacketTypes.LogType.DEBUG;
		System.arraycopy(msgBytes, 0, bytes, 2, msgBytes.length);

		MessagePacket messagePacket = MessagePacket.parse(bytes, 0, bytes.length);
		logDebug("Emitting textual log message packet: {}", messagePacket);
		notifyReceivePacket(messagePacket);

	}

	private void sendBinaryMessage(final byte binaryType, final byte[] binaryData) {

		MessagePacket messagePacket = new MessagePacket(binaryType, binaryData);

		logDebug("Emitting binary data message packet: {}", messagePacket);
		notifyReceivePacket(messagePacket);
	}

}