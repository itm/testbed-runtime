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

package de.uniluebeck.itm.wsn.devicedrivers.jennic;

import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MacAddress;
import de.uniluebeck.itm.wsn.devicedrivers.generic.Operation;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDeviceOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// -------------------------------------------------------------------------

/**
 *
 */
public class ReadMacAddressOperation extends iSenseDeviceOperation {

	// /
	private JennicDevice device;

	// /
	private MacAddress macAddress = null;

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public ReadMacAddressOperation(JennicDevice device) {
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

		device.flushReceiveBuffer();

		// Wait for a connection
		while (!isCancelled() && !device.waitForConnection())
			logInfo("Still waiting for a connection");

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
		int macStart = ChipType.getMacInFlashStart(chipType);
		int macLength = 8;
		byte[] header = device.readFlash(macStart, macLength);

		macAddress = new MacAddress(header);
		logDebug("Read MAC: " + macAddress);

		logDebug("Done, result is: " + macAddress);
		return true;
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
