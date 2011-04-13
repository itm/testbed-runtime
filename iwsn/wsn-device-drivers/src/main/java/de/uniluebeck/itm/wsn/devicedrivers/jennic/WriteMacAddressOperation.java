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

public class WriteMacAddressOperation extends iSenseDeviceOperation {

	/** */
	private JennicDevice device;

	/** */
	private MacAddress mac;

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public WriteMacAddressOperation(JennicDevice device, MacAddress mac) {
		super(device);
		this.device = device;
		this.mac = mac;
	}

	// -------------------------------------------------------------------------

	/**
	 * @return
	 * @throws Exception
	 */
	private boolean writeMac() throws Exception {

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

		// Connection established, determine chip type and configure the Flash
		// chip
		ChipType chipType = device.getChipType();
		logDebug("Chip type is " + chipType);

		// Check if the user has cancelled the operation
		if (isCancelled()) {
			logDebug("Operation has been cancelled");
			device.operationCancelled(this);
			return false;
		}

		// Read the first sector
		byte[][] sector = readSector(Sectors.SectorIndex.FIRST);

		// Check if this operation has been cancelled
		if (sector == null) {
			logDebug("Read has been cancelled");
			device.operationCancelled(this);
			return false;
		}

		// Copy address into the header of the first sector
		logDebug(
				"Copy {} to address {}, length: {}",
				StringUtils.toHexString(mac.getMacBytes()),
				ChipType.getHeaderStart(chipType),
				mac.getMacBytes().length
		);
		// System.arraycopy(mac.getMacBytes(), 0, sector[0][0], ChipType.
		// getHeaderStart(chipType),
		// mac.getMacBytes().length);
		System.arraycopy(mac.getMacBytes(), 0, sector[0], ChipType.getHeaderStart(chipType), mac.getMacBytes().length);

		// Configure flash
		device.configureFlash(chipType);

		// Erase flash sector 0
		device.eraseFlash(Sectors.SectorIndex.FIRST);

		// Write sector 0 with the new MAC
		writeSector(Sectors.SectorIndex.FIRST, sector);

		logDebug("Done, written MAC Address: " + mac);
		return true;

	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	protected byte[][] readSector(Sectors.SectorIndex index) throws Exception {
		int start = Sectors.getSectorStart(index);
		int length = Sectors.getSectorEnd(index) - start;

		// Calculate number of blocks to read
		int totalBlocks = length / getBlockSize();
		int residue = length - totalBlocks * getBlockSize();

		logDebug(String.format("length = %d, totalBlocks = %d, residue = %d", length, totalBlocks, residue));

		// Prepare byte array
		byte[][] sector = new byte[totalBlocks + (residue > 0 ? 1 : 0)][getBlockSize()];

		// Read block after block
		int address = start;
		for (int readBlocks = 0; readBlocks < totalBlocks; readBlocks++) {
			sector[readBlocks] = device.readFlash(address, getBlockSize());
			address += getBlockSize();

			float progress = ((float) readBlocks) / ((float) (totalBlocks * 2));
			device.operationProgress(Operation.WRITE_MAC, progress);

			// Check if the user has cancelled the operation
			if (isCancelled()) {
				logDebug("Operation has been cancelled");
				device.operationCancelled(this);
				return null;
			}
		}

		// Read residue
		if (residue > 0)
			sector[sector.length - 1] = device.readFlash(address, residue);

		return sector;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	private void writeSector(Sectors.SectorIndex index, byte[][] sector) throws Exception {
		int address = Sectors.getSectorStart(index);
		for (int i = 0; i < sector.length; ++i) {
			logDebug("Writing sector " + index + ", block " + i + ": " + StringUtils.toHexString(sector[i]));
			device.writeFlash(address, sector[i], 0, sector[i].length);
			address += sector[i].length;
			float progress = 0.5f + ((float) i + 1) / ((float) sector.length * 2);
			device.operationProgress(Operation.WRITE_MAC, progress);
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	private int getBlockSize() {
		return 128;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void run() {
		try {
			if (writeMac())
				operationDone(mac);
			else
				operationDone(null);

			return;
		} catch (Throwable t) {
			logError("Unhandled error in thread: " + t, t);
			operationDone(t);
		} finally {
			try {
				device.leaveProgrammingMode();
			} catch (Exception e) {
				logWarn("Failed to leave programming mode:" + e, e);
			}
		}

		// Indicate unknown failure
		operationDone(null);
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public Operation getOperation() {
		return Operation.WRITE_MAC;
	}

}
