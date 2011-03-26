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

import de.uniluebeck.itm.wsn.devicedrivers.exceptions.ProgramChipMismatchException;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.Sectors.SectorIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// -------------------------------------------------------------------------

/**
 *
 */
public class FlashProgramOperation extends iSenseDeviceOperation {

	// /
	private JennicDevice device;

	// /
	private IDeviceBinFile program;

	private boolean rebootAfterFlashing;

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public FlashProgramOperation(JennicDevice device, IDeviceBinFile program, boolean rebootAfterFlashing) {
		super(device);
		this.device = device;
		this.program = program;
		this.rebootAfterFlashing = rebootAfterFlashing;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	private boolean programFlash() throws Exception {

		JennicBinFile jennicProgram = null;

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

		// Connection established, determine chip type
		ChipType chipType = device.getChipType();
		//log.debug("Chip type is " + chipType);

		// Check if file and current chip match
		if (!program.isCompatible(chipType)) {
			logError("Chip type(" + chipType + ") and bin-program type(" + program.getFileType() + ") do not match");
			throw new ProgramChipMismatchException(chipType, program.getFileType());
		}

		// insert flash header of device
		try {

			jennicProgram = (JennicBinFile) program;

			try {
				if (!jennicProgram.insertHeader(device.getFlashHeader())) {
					logError("Unable to write flash header to binary file.");
					throw new RuntimeException("Unable to write flash header to binary file.");
				}
			} catch (Exception e) {
				logError("Unable to write flash header to binary file.");
				throw e;
			}

		} catch (ClassCastException e) {
			logError("Supplied binary file for programming the jennic device was not a jennic file. Unable to insert flash header.");
			return false;
		}

		device.configureFlash(chipType);
		device.eraseFlash(SectorIndex.FIRST);
		device.eraseFlash(SectorIndex.SECOND);
		device.eraseFlash(SectorIndex.THIRD);

		// Write program to flash
		BinFileDataBlock block = null;
		int blockCount = 0;
		while ((block = program.getNextBlock()) != null) {
			try {
				device.writeFlash(block.address, block.data, 0, block.data.length);
			} catch (Exception e) {
				logDebug("Error while reading flash! Operation will be cancelled!");
				device.operationCancelled(this);
				return false;
			}

			// Notify listeners of the new status
			float progress = ((float) blockCount) / ((float) program.getBlockCount());
			device.operationProgress(Operation.PROGRAM, progress);

			// Return with success if the user has requested to cancel this
			// operation
			if (isCancelled()) {
				logDebug("Operation has been cancelled");
				device.operationCancelled(this);
				return false;
			}

			blockCount++;
		}

		// Reboot (if requested by the user)
		if (rebootAfterFlashing) {
			logDebug("Rebooting device");
			device.reset();
		}

		return true;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public void run() {
		try {
			if (programFlash() && program != null) {
				operationDone(program);
				return;
			}
		} catch (Throwable t) {
			logError("Unhandled error in thread: " + t, t);
			operationDone(t);
			return;
		} finally {
			try {
				device.leaveProgrammingMode();
			} catch (Throwable e) {
				logWarn("Unable to leave programming mode:" + e, e);
			}
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
		return Operation.PROGRAM;
	}

}
