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

package de.uniluebeck.itm.wsn.devicedrivers.pacemate;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MacAddress;
import de.uniluebeck.itm.wsn.devicedrivers.generic.Operation;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDeviceOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// -------------------------------------------------------------------------

/**
 * @author Maick Danckwardt
 */
public class ReadMacAddressOperation extends iSenseDeviceOperation {

	private PacemateDevice device;

	private MacAddress macAddress = null;

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public ReadMacAddressOperation(PacemateDevice device) {
		super(device);
		this.device = device;
	}

	private boolean readMac() throws Exception {
		logDebug("readMac");
		// Enter programming mode
		if (!device.enterProgrammingMode()) {
			logError("Unable to enter programming mode");
			return false;
		}


		device.clearStreamData();

		device.autobaud();

		// Wait for a connection
		while (!isCancelled() && !device.waitForConnection()) {
			logInfo("Still waiting for a connection");
		}

		// Return with success if the user has requested to cancel this
		// operation
		if (isCancelled()) {
			logDebug("Operation has been cancelled");
			device.operationCancelled(this);
			return false;
		}

		// Connection established, determine chip type
		ChipType chipType = device.getChipType();
		logDebug("Chip type is " + chipType);

		// Connection established, read flash header
		long macStart = 0x2ff8;
		int macLength = 8;
		byte[] header = device.readFlash(macStart, macLength);

		byte[] macUUcode = new byte[4];
		byte[] mac = new byte[8];

		byte[] checksum = new byte[header.length - 13 - 2 - 2];
		System.arraycopy(header, 15, checksum, 0, header.length - 13 - 2 - 2);

		logDebug("Checksum: {}", StringUtils.toHexString(checksum));

		System.arraycopy(header, 1, macUUcode, 0, 4);
		byte[] macpart2 = decode(macUUcode);
		mac[0] = macpart2[0];
		mac[1] = macpart2[1];
		mac[2] = macpart2[2];

		System.arraycopy(header, 5, macUUcode, 0, 4);
		macpart2 = decode(macUUcode);
		mac[3] = macpart2[0];
		mac[4] = macpart2[1];
		mac[5] = macpart2[2];

		System.arraycopy(header, 9, macUUcode, 0, 4);
		macpart2 = decode(macUUcode);
		mac[6] = macpart2[0];
		mac[7] = macpart2[1];

		logDebug("Read raw MAC: " + StringUtils.toHexString(mac));
		macAddress = new MacAddress(mac);
		logDebug("Read MAC: " + macAddress);

		logDebug("Done, result is: " + macAddress);
		return true;
	}

	private static byte[] decode(byte[] temp) {
		byte[] outbyte = new byte[3];
		outbyte[0] = decodeByte(temp[0]);
		outbyte[1] = decodeByte(temp[1]);
		outbyte[0] <<= 2;
		outbyte[0] |= (outbyte[1] >> 4) & 0x03;
		outbyte[1] <<= 4;
		outbyte[2] = decodeByte(temp[2]);
		outbyte[1] |= (outbyte[2] >> 2) & 0x0F;
		outbyte[2] <<= 6;
		outbyte[2] |= decodeByte(temp[3]) & 0x3F;

		//System.out.println(" = "+(int)(outbyte[0] & 0xFF)+" "+(int)(outbyte[1] & 0xFF)+" "+(int)(outbyte[2] & 0xFF));
		//checksum2 = checksum2 + (int)(outbyte[0] & 0xFF)+(int)(outbyte[1] & 0xFF)+(int)(outbyte[2] & 0xFF);
		return outbyte;
	}

	private static byte decodeByte(byte b) {
		if (b == 0x60) {
			return 0;
		} else {
			return (byte) (b - 0x20);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void run() {
		try {
			logDebug("starting mac read operation");
			if (readMac() && macAddress != null) {
				try {
					device.leaveProgrammingMode();
				} catch (Exception e) {
					logWarn("Failed to leave programming mode:" + e, e);
				}
				operationDone(macAddress);
				return;
			}
		} catch (Throwable t) {
			logError("Unhandled error in thread: " + t, t);
			operationDone(t);
			return;
		}

		// Indicate failure
		operationDone(null);
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public Operation getOperation() {
		return Operation.READ_MAC;
	}

}
