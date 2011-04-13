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

package de.uniluebeck.itm.wsn.devicedrivers.telosb;

import de.uniluebeck.itm.wsn.devicedrivers.exceptions.FlashProgramFailedException;
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.ProgramChipMismatchException;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Friedemann Wesner
 */
public class TelosbFlashProgramOperation extends iSenseDeviceOperation {

	private IDeviceBinFile binFile = null;

	/**
	 * Constructor
	 *
	 * @param device
	 * @param binFile
	 */
	public TelosbFlashProgramOperation(iSenseDeviceImpl device, IDeviceBinFile binFile) {
		super(device);

		if (device == null || binFile == null) {
			throw new NullPointerException("Supplied device or binFile for FlashProgramOperation is null");
		}

		this.binFile = binFile;
	}

	/* (non-Javadoc)
	 * @see ishell.device.iSenseDeviceOperation#getOperation()
	 */

	@Override
	public Operation getOperation() {
		return Operation.PROGRAM;
	}

	/* (non-Javadoc)
	 * @see ishell.device.iSenseDeviceOperation#run()
	 */

	@Override
	public void run() {
		try {
			if (programFlash()) {
				operationDone(binFile);
				return;
			} else {
				logError("Failed to program flash.");
			}
		} catch (Exception e) {
			logError("Unhandled error while programming flash: " + e, e);
			operationDone(e);
		} finally {
			try {
				getDevice().leaveProgrammingMode();
			} catch (Throwable e) {
				logWarn("Unable to leave programming mode:" + e, e);
			}
		}

		// Indicate failure
		operationDone(null);
	}

	private boolean programFlash() throws Exception {
		ChipType chipType;
		BinFileDataBlock block;
		int blockCount = 0;
		float progress = 0f;
		int bytesProgrammed = 0;

		// Return if the user has requested to cancel this operation
		if (isCancelled()) {
			logDebug("Operation has been cancelled");
			getDevice().operationCancelled(this);
			return false;
		}

		// enter programming mode
		try {
			if (!getDevice().enterProgrammingMode()) {
				logError("Unable to enter programming mode");
				return false;
			}
		} catch (Exception e) {
			logError("Error on entering programming mode: " + e, e);
			return false;
		}

		// Check if file and current chip match
		chipType = getDevice().getChipType();
		if (!binFile.isCompatible(chipType)) {
			logError("Chip type(" + chipType + ") and bin-program type(" + binFile.getFileType() + ") do not match");
			throw new ProgramChipMismatchException(chipType, binFile.getFileType());
		}

		// Write program to flash
		logInfo("Starting to write program into flash memory...");
		while ((block = binFile.getNextBlock()) != null) {

			// write single block
			try {
				getDevice().writeFlash(block.address, block.data, 0, block.data.length);
			} catch (FlashProgramFailedException e) {
				logError(String.format("Error writing %d bytes into flash " +
						"at address 0x%02x: " + e + ". Programmed " + bytesProgrammed + " bytes so far. " +
						". Operation will be canceled.", block.data.length, block.address), e);
				getDevice().operationCancelled(this);
				return false;
			} catch (IOException e) {
				logError("I/O error while writing flash: " + e + ". Programmed " + bytesProgrammed + " bytes so far. " +
						"Operation will be canceled!", e);
				getDevice().operationCancelled(this);
				return false;
			}

			bytesProgrammed += block.data.length;

			// Notify listeners of the new status
			progress = ((float) blockCount) / ((float) binFile.getBlockCount());
			getDevice().operationProgress(Operation.PROGRAM, progress);

			// Return if the user has requested to cancel this operation
			if (isCancelled()) {
				getDevice().operationCancelled(this);
				return false;
			}

			blockCount++;
		}

		// reset device (exit boot loader)
		logInfo("Resetting device.");
		try {
			if (!getDevice().reset()) {
				logWarn("Failed to reset device after programming flash!");
				return false;
			}
		} catch (Exception e) {
			logError("Error while resetting device: " + e, e);
			return false;
		}

		logDebug("Programmed " + bytesProgrammed + " bytes.");

		return true;
	}

}
