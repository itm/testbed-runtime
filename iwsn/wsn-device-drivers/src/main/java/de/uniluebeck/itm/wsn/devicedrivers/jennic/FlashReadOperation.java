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

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.Operation;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDeviceOperation;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.Sectors.SectorIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// -------------------------------------------------------------------------

/**
 *
 */
public class FlashReadOperation extends iSenseDeviceOperation {

	// /
	private JennicDevice device;

	/** */
	private int startAddress = Sectors.getSectorStart(SectorIndex.FIRST);

	/** */
	private int endAddress = Sectors.getSectorEnd(SectorIndex.FOURTH);

	private byte result[] = null;

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public FlashReadOperation(JennicDevice device, int startAddress, int endAddress) {
		super(device);
		this.device = device;
		this.startAddress = startAddress;
		this.endAddress = endAddress;
	}

	private boolean readFlash() throws Exception {
		// Enter programming mode
		if (!device.enterProgrammingMode()) {
			logError("Unable to enter programming mode");
			return false;
		}

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

		// Read all sectors
		byte flashData[] = new byte[endAddress - startAddress + 1];
		try {
			int sectorStart = startAddress;
			int sectorEnd = endAddress;

			while (sectorStart < sectorEnd) {
				// Determine length of the data block to read
				int length = 32;
				if (sectorStart + 32 > sectorEnd)
					length = sectorEnd - sectorStart;

				// Read data block
				try {
					byte[] data = device.readFlash(sectorStart, length);
					System.arraycopy(data, 0, flashData, sectorStart - startAddress, data.length);
				} catch (Exception e) {
					logDebug("Error while reading flash! Operation will be cancelled!");
					device.operationCancelled(this);
					return false;
				}
				// Notify listeners
				float progress = ((float) (sectorStart - startAddress)) / ((float) (endAddress - startAddress));
				device.operationProgress(Operation.READ_FLASH, progress);

				// Increment start address
				sectorStart += length;

			}
			logDebug("Done, result is: " + StringUtils.toHexString(flashData));
			result = flashData;
			return true;
		} catch (Exception e) {
			logError("Error while reading flash contents: " + e, e);
			return false;
		} finally {
			device.leaveProgrammingMode();
		}

	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void run() {
		try {
			if (readFlash() && result != null) {
				operationDone(result);
				return;
			}
		} catch (Throwable t) {
			logError("Unhandled error in thread: " + t, t);
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
		return Operation.READ_FLASH;
	}
}
