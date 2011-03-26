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


import de.uniluebeck.itm.wsn.devicedrivers.nulldevice.NullDevice;
import gnu.io.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dp
 */
public abstract class iSenseDevice {
	
	/** */
	private static final Logger log = LoggerFactory.getLogger(iSenseDevice.class);

	// -------------------------------------------------------------------------

	/**
	 * @param p
	 * @throws Exception
	 */
	public abstract void send(MessagePacket p) throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * Sets the MessageMode for all connections Plain or Packet
	 *
	 * @param messageMode the new MessageMode
	 */
	public abstract void setReceiveMode(MessageMode messageMode);

	// -------------------------------------------------------------------------

	/**
	 * @param program
	 * @return
	 * @throws Exception
	 */
	public abstract boolean triggerProgram(IDeviceBinFile program, boolean rebootAfterFlashing) throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @param mac
	 * @throws Exception
	 */
	public abstract void triggerSetMacAddress(MacAddress mac, boolean rebootAfterFlashing) throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @throws Exception
	 */
	public abstract void triggerGetMacAddress(boolean rebootAfterFlashing) throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @throws Exception
	 */
	public abstract boolean triggerReboot() throws Exception;

	// -------------------------------------------------------------------------

	/**
	 * @param listener
	 */
	public abstract void registerListener(iSenseDeviceListener listener);

	// -------------------------------------------------------------------------

	/**
	 * @param listener
	 */
	public abstract void deregisterListener(iSenseDeviceListener listener);

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public abstract void registerListener(iSenseDeviceListener listener, int type);

	// -------------------------------------------------------------------------

	/**
	 */
	public abstract void deregisterListener(iSenseDeviceListener listener, int type);

	// ------------------------------------------------------------------------
	// --

	/**
	 *
	 */
	public abstract void cancelOperation(Operation op);

	/**
	 * If the device has a serial port it will be returned. Else null will be
	 * returned
	 *
	 * @return
	 */
	public SerialPort getSerialPort() {
		return null;
	}

	/**
	 * Returns the channels
	 *
	 * @return
	 */
	public abstract int[] getChannels();

	/**
	 * Create and return an appropriate binary file corresponding to the type of this device
	 *
	 * @param fileName path to the file to be loaded
	 * @return the created binary file
	 * @throws Exception
	 */
	public abstract IDeviceBinFile loadBinFile(String fileName) throws Exception;

	/**
	 *
	 */
	public abstract void shutdown();

	public abstract boolean isConnected();

	/**
	 * Sets a string that will be printed before every logging statement made. This may e.g. help to identify an
	 * individual devices logging output if multiple devices are running concurrently.
	 *
	 * @param logIdentifier the string to be used as logging statement prefix
	 */
	public abstract void setLogIdentifier(String logIdentifier);

}