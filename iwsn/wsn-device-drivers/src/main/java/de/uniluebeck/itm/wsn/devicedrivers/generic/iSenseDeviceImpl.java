/**********************************************************************************************************************
 * Copyright (c) 2010, coalesenses GmbH                                                                               *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the coalesenses GmbH nor the names of its contributors may be used to endorse or promote     *
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

package de.uniluebeck.itm.wsn.devicedrivers.generic;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.FlashType;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.Sectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author dp
 */
public abstract class iSenseDeviceImpl extends iSenseDevice {

	/** */
	private final Logger log = LoggerFactory.getLogger(getClass());

	/** */
	protected static final byte DLE = 0x10;

	/** */
	protected static final byte[] DLE_STX = new byte[]{DLE, 0x02};

	/** */
	protected static final byte[] DLE_ETX = new byte[]{DLE, 0x03};

	/**
	 * A list of listeners that get reported ALL traffic there is, not just individual packet types they registered for.
	 * The list is immutable so that listeners can de-register themselves while being called, thereby not producing
	 * {@link ConcurrentModificationException}s.
	 */
	protected ImmutableList<iSenseDeviceListener> promiscuousListeners = ImmutableList.of();

	/**
	 * Lock that has to be acquired if the promiscuousListeners variable is to be changed
	 */
	protected final Lock promiscuousListenersLock = new ReentrantLock();

	/**
	 * A map that contains for each entry a list of listeners that registered themselves for a specific packet type.
	 * The packet type is the key of the map. The lists inside are immutable so that listeners can de-register
	 * themselves while being called, thereby not producing {@link ConcurrentModificationException}s.
	 */
	protected ImmutableMap<Integer, ImmutableList<iSenseDeviceListener>> listeners = ImmutableMap.of();

	/**
	 * Lock that has to be acquired if the listeners variable is to be changed
	 */
	protected final Lock listenersLock = new ReentrantLock();

	/** */
	protected int timeoutMillis = 2000;

	/**
	 * Data buffer for incoming data
	 */
	private byte[] packet = new byte[2048];

	/**
	 * Current packetLength of the received packet
	 */
	private int packetLength = 0;

	/** */
	private boolean foundDLE = false;

	/** */
	private boolean foundPacket = false;

	/** */
	protected iSenseDeviceOperation operation = null;

	/**
	 * Message mode
	 */
	protected MessageMode messageMode = MessageMode.PACKETS;

	/** */
	private boolean operationCancelRequestActive = false;

	/**
	 * Tells the device to hide debug output during the flash operation and not to verify written blocks and upload bsl
	 * patches each time (mainly for telosB device)
	 */
	protected boolean flashDebugOutput = false;

	protected TimeDiff operationProgressTimeDiff = new TimeDiff(100);

	protected String logIdentifier;

	public boolean isFlashDebugOutput() {
		return flashDebugOutput;
	}

	public void setFlashDebugOutput(boolean flashDebugOutput) {
		this.flashDebugOutput = flashDebugOutput;
	}

	@Override
	public abstract void send(MessagePacket p) throws Exception;

	public abstract void leaveProgrammingMode() throws Exception;

	@Override
	public void registerListener(iSenseDeviceListener listener) {

		try {
			// acquire lock first so that we don't get any lost updates
			promiscuousListenersLock.lock();

			// copy all current listeners to new immutable list
			ImmutableList<iSenseDeviceListener> newPromiscuousListeners =
					ImmutableList.<iSenseDeviceListener>builder().addAll(promiscuousListeners).add(listener).build();

			// exchange old and new list
			promiscuousListeners = newPromiscuousListeners;

		} finally {
			promiscuousListenersLock.unlock();
		}

		// log the update
		logDebug("Added promiscuous listener {}, now got {} listeners", listener, promiscuousListeners.size());
	}

	@Override
	public void deregisterListener(iSenseDeviceListener listener) {

		try {

			// acquire lock first so that we don't get any lost updates
			promiscuousListenersLock.lock();

			// copy all current listeners except the one to remove to new immutable list
			ImmutableList.Builder<iSenseDeviceListener> newPromiscuousListenersBuilder = ImmutableList.builder();

			for (iSenseDeviceListener promiscuousListener : promiscuousListeners) {
				if (promiscuousListener != listener) {
					newPromiscuousListenersBuilder.add(promiscuousListener);
				}
			}

			// exchange old and new list
			promiscuousListeners = newPromiscuousListenersBuilder.build();

		} finally {
			promiscuousListenersLock.unlock();
		}

		// log the update
		logDebug("Removed promiscuous listener {}, now got {} listeners", listener, promiscuousListeners.size());

		// de-register listener of all types he subscribed
		for (Integer type : listeners.keySet()) {
			deregisterListener(listener, type);
		}

	}

	@Override
	public void registerListener(iSenseDeviceListener listener, int type) {

		try {

			// acquire lock first so that we don't get any lost updates
			listenersLock.lock();

			ImmutableMap.Builder<Integer, ImmutableList<iSenseDeviceListener>> newMap = ImmutableMap.builder();

			// copy all references of lists contained in the map to the new map but construct new list with listener
			// to add if it's the same type (map key)
			for (Map.Entry<Integer, ImmutableList<iSenseDeviceListener>> entry : listeners.entrySet()) {

				if (entry.getKey() == type) {

					// copy all current listeners into a new immutable list and add new listener
					ImmutableList.Builder<iSenseDeviceListener> newListeners =
							ImmutableList.<iSenseDeviceListener>builder().add(listener);

					ImmutableList<iSenseDeviceListener> currentListeners = listeners.get(type);
					if (currentListeners != null) {
						newListeners.addAll(currentListeners);
					}

					// exchange old and new list
					newMap.put(entry.getKey(), newListeners.build());

				} else {
					newMap.put(entry.getKey(), entry.getValue());
				}
			}

			// exchange old and new map
			listeners = newMap.build();


		} finally {
			listenersLock.unlock();
		}

		// log the update
		logDebug("Added listener {} for type {}, now got {} listeners", listener, type, this.listeners.get(type).size());
	}

	public void deregisterListener(iSenseDeviceListener listener, int type) {

		try {

			// acquire lock first so that we don't get any lost updates
			listenersLock.lock();

			ImmutableMap.Builder<Integer, ImmutableList<iSenseDeviceListener>> newMap = ImmutableMap.builder();

			// copy all references of lists contained in the map to the new map but construct new list without
			// listener to remove if it's the same type (map key)
			for (Map.Entry<Integer, ImmutableList<iSenseDeviceListener>> entry : listeners.entrySet()) {

				if (entry.getKey() == type) {

					// copy all current listeners except the one to remove into a new immutable list
					ImmutableList.Builder<iSenseDeviceListener> newListeners = ImmutableList.builder();

					for (iSenseDeviceListener currentListener : entry.getValue()) {
						if (currentListener != listener) {
							newListeners.add(currentListener);
						}
					}

					// exchange old and new list
					newMap.put(entry.getKey(), newListeners.build());

				} else {
					newMap.put(entry.getKey(), entry.getValue());
				}
			}

			// exchange old and new map
			listeners = newMap.build();

		} finally {
			listenersLock.unlock();
		}

		// log the update
		logDebug("Removed listener {} for type {}, now got {} listeners.", listener, type, listeners.get(type).size());
	}

	// ------------------------------------------------------------------------
	// --
	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDevice#cancelOperation(ishell.device.Operation)
	 */

	public void cancelOperation(Operation op) {

		if (operationInProgress() && getOperation() == op) {

			logInfo("Operation {} of type {} will be cancelled.", operation, operation.getOperation());

			operationCancelRequestActive = true;
			if (operation != null) {
				operation.cancelOperation();
			}
		} else {
			/*if(operation!=null)
			{
				operation.cancelOperation();
			}*/
			logWarn("No operation of type {} to cancel", op);
		}

	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	protected boolean operationInProgress() {
		boolean op = (operation != null) || (operationCancelRequestActive == true);
		//log.debug("Operation in progress: " + op);
		return op;

	}

	// -------------------------------------------------------------------------

	/**
	 * @return
	 */
	public abstract Operation getOperation();

	// -------------------------------------------------------------------------

	/**
	 * @return
	 * @throws Exception
	 */
	public abstract boolean reset() throws Exception;

	// -------------------------------------------------------------------------

	// -------------------------------------------------------------------------

	/**
	 * @return
	 * @throws Exception
	 */
	public abstract boolean enterProgrammingMode() throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @throws Exception
	 */
	public abstract void eraseFlash() throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @param sector
	 * @throws Exception
	 */
	public abstract void eraseFlash(Sectors.SectorIndex sector) throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @param address
	 * @param bytes
	 * @param offset
	 * @param len
	 * @return
	 * @throws Exception
	 */
	public abstract byte[] writeFlash(int address, byte bytes[], int offset, int len) throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @param address
	 * @param len
	 * @return
	 * @throws Exception
	 */
	public abstract byte[] readFlash(int address, int len) throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @return
	 * @throws Exception
	 */
	public abstract ChipType getChipType() throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @return
	 * @throws Exception
	 */
	public abstract FlashType getFlashType() throws Exception;

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public iSenseDeviceImpl() {
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void operationDone(final Operation op, final Object result) {

		if (!(result instanceof IDeviceBinFile)) {
			logDebug("Operation {} done, result: {}", op, result);
		} else {
			logDebug("Operation {} done.", op);
		}

		for (final iSenseDeviceListener l : promiscuousListeners) {
			l.operationDone(op, result);
		}

		for (List<iSenseDeviceListener> ls : listeners.values()) {
			for (final iSenseDeviceListener l : ls) {
				l.operationDone(op, result);
			}
		}

		operation = null;
		operationCancelRequestActive = false;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	public void operationCancelled(final iSenseDeviceOperation op) {

		logDebug("Operation {} cancelled", op);

		for (final iSenseDeviceListener l : promiscuousListeners) {
			l.operationCanceled(op.getOperation());
		}

		for (List<iSenseDeviceListener> ls : listeners.values()) {
			for (final iSenseDeviceListener l : ls) {
				l.operationCanceled(op.getOperation());
			}
		}

		operation = null;

	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void operationProgress(final Operation op, final float fraction) {

		if (log.isDebugEnabled() && operationProgressTimeDiff.isTimeout()) {
			operationProgressTimeDiff.touch();
			logDebug("Operation {} progress: {}", op, fraction);
		}

		for (final iSenseDeviceListener l : promiscuousListeners) {
			l.operationProgress(op, fraction);
		}

		for (List<iSenseDeviceListener> ls : listeners.values()) {
			for (final iSenseDeviceListener l : ls) {
				l.operationProgress(op, fraction);
			}
		}

	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public int getTimeoutMillis() {
		return timeoutMillis;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void setTimeoutMillis(int timeoutMillis) {
		this.timeoutMillis = timeoutMillis;
	}

	public void setReceiveMode(MessageMode messageMode) {
		this.messageMode = messageMode;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	protected void receive(InputStream inStream) {

		if (messageMode == MessageMode.PACKETS) {
			receivePacket(inStream);
		} else if (messageMode == MessageMode.PLAIN) {
			receivePlainText(inStream);
		} else {
			logError("Unknown message type: {}", getSerialPort(), messageMode);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	private void receivePacket(InputStream inStream) {
		try {
			while (inStream != null && inStream.available() > 0) {
				byte c = (byte) (0xff & inStream.read());

				// Check if DLE was found
				if (foundDLE) {
					foundDLE = false;

					if (c == MessagePacket.STX && !foundPacket) {
						//log.debug("iSenseDeviceImpl: STX received in DLE mode");
						foundPacket = true;
					} else if (c == MessagePacket.ETX && foundPacket) {
						//log.debug("ETX received in DLE mode");

						// Parse message and notify listeners
						MessagePacket p = MessagePacket.parse(packet, 0, packetLength);
						// p.setIsenseDevice(this);
						//log.debug("Packet found: " + p);
						notifyReceivePacket(p);

						// Reset packet information
						clearPacket();
					} else if (c == MessagePacket.DLE && foundPacket) {
						// Stuffed DLE found
						//log.debug("Stuffed DLE received in DLE mode");
						ensureBufferSize();
						packet[packetLength++] = MessagePacket.DLE;
					} else {

						if (log.isErrorEnabled()) {
							logError(
									"Incomplete packet received: \nHEX: {}\nSTR: {}",
									StringUtils.toHexString(this.packet, 0, packetLength),
									new String(this.packet, 0, packetLength)
							);
						}
						clearPacket();
					}

				} else {
					if (c == MessagePacket.DLE) {
						//log.debug("Plain DLE received");
						foundDLE = true;
					} else if (foundPacket) {
						ensureBufferSize();
						packet[packetLength++] = c;
					} else {
						// Plain Message e.g. from Contiki or iSense
						// Read all available characters
						packetLength = 0;
						while (inStream != null && inStream.available() != 0 && (packetLength + 1) < packet.length) {
							packet[packetLength++] = (byte) (0xFF & inStream.read());
						}

						// Copy them into a buffer with correct length
						byte[] buffer = new byte[packetLength];
						System.arraycopy(packet, 0, buffer, 0, packetLength);

						MessagePacket p = new MessagePacket(PacketTypes.LOG, buffer);
						notifyReceivePacket(p);
					}

				}
			}

		} catch (IOException error) {
			logError("Error on rx (Retry in 1s): {}", error);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logError("" + e);
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	private void receivePlainText(InputStream inStream) {
		try {
			// Read all available characters
			packetLength = 0;
			while (inStream != null && inStream.available() != 0 && (packetLength + 1) < packet.length) {
				packet[packetLength++] = (byte) (0xFF & inStream.read());
			}

			// Copy them into a buffer with correct length
			byte[] buffer = new byte[packetLength];
			System.arraycopy(packet, 0, buffer, 0, packetLength);

			// Notify listeners
			MessagePlainText p = new MessagePlainText(buffer);
			notifyReceivePlainText(p);

			// Reset packet information
			packetLength = 0;
		} catch (IOException error) {
			logDebug("Error while receiving plain text packet: {}", error);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void notifyReceivePacket(final MessagePacket p) {

		for (final iSenseDeviceListener l : promiscuousListeners) {
			l.receivePacket(p);
		}

		List<iSenseDeviceListener> ls = this.listeners.get(p.getType());
		if (ls != null) {
			for (final iSenseDeviceListener l : ls) {
				l.receivePacket(p);
			}
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void notifyReceivePlainText(final MessagePlainText p) {

		logDebug("New plain text packet received: {}", p);

		for (final iSenseDeviceListener l : promiscuousListeners) {
			l.receivePlainText(p);
		}

	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	protected void clearPacket() {
		packetLength = 0;
		foundDLE = false;
		foundPacket = false;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	protected void ensureBufferSize() {
		if (packetLength + 1 >= this.packet.length) {
			byte tmp[] = new byte[packetLength + 100];
			System.arraycopy(this.packet, 0, tmp, 0, packetLength);
			this.packet = tmp;
		}
	}

	@Override
	public void setLogIdentifier(String logIdentifier) {
		this.logIdentifier = logIdentifier == null ? null : logIdentifier.endsWith(" ") ? logIdentifier : logIdentifier + " ";
	}

	protected void logDebug(String format, Object... args) {
		if (log.isDebugEnabled()) {
			if (logIdentifier != null) {
				log.debug(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = this.getClass().getSimpleName();
				newArgs[1] = getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.debug("[{},{}] " + format, newArgs);
			}
		}
	}

	protected void logTrace(String format, Object... args) {
		if (log.isTraceEnabled()) {
			if (logIdentifier != null) {
				log.trace(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = this.getClass().getSimpleName();
				newArgs[1] = getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.trace("[{},{}] " + format, newArgs);
			}
		}
	}

	protected void logInfo(String format, Object... args) {
		if (log.isInfoEnabled()) {
			if (logIdentifier != null) {
				log.info(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = this.getClass().getSimpleName();
				newArgs[1] = getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.info("[{},{}] " + format, newArgs);
			}
		}
	}

	protected void logWarn(String format, Object... args) {
		if (log.isWarnEnabled()) {
			if (logIdentifier != null) {
				log.warn(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = this.getClass().getSimpleName();
				newArgs[1] = getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.warn("[{},{}] " + format, newArgs);
			}
		}
	}

	protected void logError(String format, Object... args) {
		if (log.isErrorEnabled()) {
			if (logIdentifier != null) {
				log.error(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = this.getClass().getSimpleName();
				newArgs[1] = getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.error("[{},{}] " + format, newArgs);
			}
		}
	}

}
