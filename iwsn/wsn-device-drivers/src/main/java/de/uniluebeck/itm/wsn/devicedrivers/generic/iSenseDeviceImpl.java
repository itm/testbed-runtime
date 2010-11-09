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


import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.FlashType;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.Sectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author dp
 */
public abstract class iSenseDeviceImpl extends iSenseDevice {
	/** */
	private static final Logger log = LoggerFactory.getLogger(iSenseDeviceImpl.class);

	/** */
	protected static final byte DLE = 0x10;

	/** */
	protected static final byte[] DLE_STX = new byte[]{DLE, 0x02};

	/** */
	protected static final byte[] DLE_ETX = new byte[]{DLE, 0x03};

	/** */
	protected Set<iSenseDeviceListener> promiscousListeners = new TreeSet<iSenseDeviceListener>();

	/** */
	protected Map<Integer, Set<iSenseDeviceListener>> listeners = new HashMap<Integer, Set<iSenseDeviceListener>>();

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
	 * Tells the device to hide debug output during the flash operation 
	 * and not to verify written blocks and upload bsl patches each time (mainly for telosB device)  
	 */
	protected boolean flashDebugOutput = false;

	/**
	 * @return
	 */
	public boolean isFlashDebugOutput() {
		return flashDebugOutput;
	}

	/**
	 * @param flashDebugOutput
	 */
	public void setFlashDebugOutput(boolean flashDebugOutput) {
		this.flashDebugOutput = flashDebugOutput;
	}

	// -------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @see ishell.device.iSenseDevice#send(ishell.util.messages.Packet)
	 */

	public abstract void send(MessagePacket p) throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @throws Exception
	 */
	public abstract void leaveProgrammingMode() throws Exception;

	// -------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @seeishell.device.iSenseDevice#registerListener(ishell.device.
	 * iSenseDeviceListener)
	 */

	public void registerListener(iSenseDeviceListener listener) {
//		log.debug("Added promiscous listener " + listener + ", already got " + promiscousListeners.size() + " listeners");
		promiscousListeners.add(listener);
	}

	// -------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @seeishell.device.iSenseDevice#deregisterListener(ishell.device.
	 * iSenseDeviceListener)
	 */

	public void deregisterListener(iSenseDeviceListener listener) {
        removeListenerInternal(promiscousListeners, listener);
		for (Set<iSenseDeviceListener> ls : listeners.values())
			removeListenerInternal(ls, listener);
	}

    /**
     * Removes a listener from a set by checking for object identity. The remove operation of Java Treeset requires the
     * listener to implement Comparable which leads to errors if they don't.
     *
     * @param set
     * @param listener
     */
    private void removeListenerInternal(Set<iSenseDeviceListener> set, iSenseDeviceListener listener) {
        Iterator<iSenseDeviceListener> iterator = set.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == listener) {
                iterator.remove();
            }
        }
    }

	// -------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @seeishell.device.iSenseDevice#registerListener(ishell.device.
	 * iSenseDeviceListener, int)
	 */

	public void registerListener(iSenseDeviceListener listener, int type) {
		Set<iSenseDeviceListener> s = listeners.get(type);
		if (s == null) {
			s = new TreeSet<iSenseDeviceListener>();
			listeners.put(type, s);
//			log.debug("Created first listener set for type " + type);
		}
		s.add(listener);
//		log.debug("Added listener " + listener + " for type " + type + ", now got " + listeners.size() + " listeners");
	}

	// -------------------------------------------------------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @seeishell.device.iSenseDevice#deregisterListener(ishell.device.
	 * iSenseDeviceListener, int)
	 */

	public void deregisterListener(iSenseDeviceListener listener, int type) {
		Set<iSenseDeviceListener> s = listeners.get(type);
		if (s == null) {
			log.debug("Listener " + listener + " not registered for type " + type);
		} else {
			s.remove(listener);
//			log.debug("Removed listener " + listener + " for type " + type + ", now got " + listeners.size() + " listeners");
		}
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 * @param d
	 */
	public void copyListeners(iSenseDevice d) {
		Set<iSenseDeviceListener> count = new TreeSet<iSenseDeviceListener>();

		// Register promiscous listeners
		for (iSenseDeviceListener l : promiscousListeners) {
			d.registerListener(l);
			count.add(l);
		}

		// Register other listeners
		for (Integer i : listeners.keySet())
			for (iSenseDeviceListener l : listeners.get(i)) {
				d.registerListener(l, i);
				count.add(l);
			}

		log.debug("Copied " + count.size() + " unique listeners from " + this + " to " + d);
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
			log.info("Operation " + operation + " of type " + operation.getOperation() + " will be cancelled.");
			operationCancelRequestActive = true;
			if (operation != null)
				operation.cancelOperation();
		} else {
			/*if(operation!=null)
			{
				operation.cancelOperation();
			}*/
			log.warn("No operation of type " + op + " to cancel");
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
	public void operationDone(Operation op, Object result) {
		if (log.isDebugEnabled()) {
			if (!(result instanceof IDeviceBinFile)) {
				log.debug("Operation " + op + " done, result: " + result);
			} else {
				log.debug("Operation " + op + " done.");
			}
		}

		for (iSenseDeviceListener l : promiscousListeners)
			l.operationDone(op, result);

		for (Set<iSenseDeviceListener> ls : listeners.values())
			for (iSenseDeviceListener l : ls)
				l.operationDone(op, result);

		operation = null;
		operationCancelRequestActive = false;
	}

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	public void operationCancelled(iSenseDeviceOperation op) {
		if (log.isDebugEnabled())
			log.debug("Operation " + op + " cancelled");

		for (iSenseDeviceListener l : promiscousListeners)
			l.operationCanceled(op.getOperation());

		for (Set<iSenseDeviceListener> ls : listeners.values())
			for (iSenseDeviceListener l : ls)
				l.operationCanceled(op.getOperation());

		operation = null;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void operationProgress(Operation op, float fraction) {
		if (log.isDebugEnabled())
			log.debug("Operation " + op + " progress: " + fraction);

		for (iSenseDeviceListener l : promiscousListeners)
			l.operationProgress(op, fraction);

		for (Set<iSenseDeviceListener> ls : listeners.values())
			for (iSenseDeviceListener l : ls)
				l.operationProgress(op, fraction);
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

		if (messageMode == MessageMode.PACKETS)
			receivePacket(inStream);
		else if (messageMode == MessageMode.PLAIN)
			receivePlainText(inStream);
		else
			log.error("Unknown message type [" + messageMode + "]");
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
						log.error("iSenseDeviceImpl: Incomplete packet received: " + StringUtils.toHexString(this.packet, 0, packetLength));
						log.error("String representation: " + new String(this.packet, 0, packetLength));
						clearPacket();
					}

				} else {
					if (c == MessagePacket.DLE) {
						//log.debug("Plain DLE received");
						foundDLE = true;
					} else if (foundPacket) {
						ensureBufferSize();
						packet[packetLength++] = c;
					}
				}
			}

		} catch (IOException error) {
			log.error("Error on rx (Retry in 1s): " + error, error);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
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
			while (inStream != null && inStream.available() != 0 && (packetLength + 1) < packet.length)
				packet[packetLength++] = (byte) (0xFF & inStream.read());

			// Copy them into a buffer with correct length
			byte[] buffer = new byte[packetLength];
			System.arraycopy(packet, 0, buffer, 0, packetLength);

			// Notify listeners
			MessagePlainText p = new MessagePlainText(buffer);
			notifyReceivePlainText(p);

			// Reset packet information
			packetLength = 0;
		} catch (IOException error) {
			log.debug("Error: " + error, error);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void notifyReceivePacket(MessagePacket p) {
		//if (log.isDebugEnabled())
		//log.debug("New packet received: type =" + p.getType() + " packet: " + p.toString());

		// TODO: call PacketDispatcher

		for (iSenseDeviceListener l : promiscousListeners) {
			/*
			if (l instanceof Plugin)
			{
				try {
					((Plugin)l).handlePacket(p);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else */
			l.receivePacket(p);
		}

		Set<iSenseDeviceListener> ls = this.listeners.get(p.getType());
		if (ls != null)
			for (iSenseDeviceListener l : ls) {
				/*
				if (l instanceof Plugin)
				{
					try {
						((Plugin)l).handlePacket(p);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else */
				l.receivePacket(p);
			}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void notifyReceivePlainText(MessagePlainText p) {
		if (log.isDebugEnabled())
			log.debug("New plain text packet received: " + p);

		for (iSenseDeviceListener l : promiscousListeners) {
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

}
