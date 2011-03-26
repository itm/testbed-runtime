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

import java.io.IOException;

import de.uniluebeck.itm.wsn.devicedrivers.exceptions.InvalidChecksumException;
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.ProgramChipMismatchException;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlashProgramOperation extends iSenseDeviceOperation {

	private PacemateDevice device;

	private IDeviceBinFile program;

	private boolean rebootAfterFlashing;

	public FlashProgramOperation(PacemateDevice device, IDeviceBinFile program, boolean rebootAfterFlashing) {
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
		PacemateBinFile pacemateProgram = null;
		// Enter programming mode
		if (!device.enterProgrammingMode()) {
			logError("Unable to enter programming mode");
			return false;
		}
		try {
			device.clearStreamData();
		} catch (IOException e) {
			logError("Error while clear stream"+e);
			e.printStackTrace();
		}
		logDebug("autobaud");
		device.autobaud();
		logDebug("autobaud ready");
		// device.echoOff();

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
		// logDebug("Chip type is " + chipType);

		// Check if file and current chip match
		if (!program.isCompatible(chipType)) {
			logError("Chip type(" + chipType + ") and bin-program type(" + program.getFileType() + ") do not match");
			throw new ProgramChipMismatchException(chipType, program.getFileType());
		}

		pacemateProgram = (PacemateBinFile) program;

		// pacemateProgram.changeStrangeBytePattern();

		try {
			device.configureFlash();
		} catch (Exception e) {
			logDebug("Error while configure flash! Operation will be cancelled!");
			device.operationCancelled(this);
			return false;
		}

		try {
			device.eraseFlash();
		} catch (Exception e) {
			logDebug("Error while erasing! Operation will be cancelled!");
			device.operationCancelled(this);
			return false;
		}

		int flash_crc = pacemateProgram.calcCRC();

		System.out.println("CRC " + flash_crc);

		try {
			device.writeCRCtoFlash(flash_crc);
		} catch (Exception e) {
			logDebug("Error while write CRC to Flash! Operation will be cancelled!");
			device.operationCancelled(this);
			return false;
		}

		// Write program to flash
		BinFileDataBlock block = null;
		int blockCount = 3;
		int blockNumber = 3; // blockNumber != blockCount because block 8 & 9 == 32 kb all other 4 kb
		while ((block = program.getNextBlock()) != null) {
			try {
				device.writeToRAM(device.startAdressInRam, block.data.length);
			} catch (Exception e) {
				logDebug("Error while write to RAM! Operation will be cancelled!"+e);
				device.operationCancelled(this);
				return false;
			}

			int counter = 0;
			int linecounter = 0;

			byte[] line = null;

			// each block is sent in parts of 20 lines a 45 bytes
			while (counter < block.data.length) {
				int offset = 0;
				int bytesNotYetProoved = 0;
				if (counter + 45 < block.data.length) {
					line = new byte[PacemateBinFile.linesize]; // a line with 45 bytes
					System.arraycopy(block.data, counter, line, 0, PacemateBinFile.linesize);
					counter = counter + PacemateBinFile.linesize;
					bytesNotYetProoved = bytesNotYetProoved + PacemateBinFile.linesize;
				} else {
					if (((block.data.length - counter) % 3) == 1)
						offset = 2;
					else if (((block.data.length - counter) % 3) == 2)
						offset = 1;
					line = new byte[block.data.length - counter + offset];
					line[line.length - 1] = 0;
					line[line.length - 2] = 0;
					System.arraycopy(block.data, counter, line, 0, block.data.length - counter);
					counter = counter + (block.data.length - counter);
					bytesNotYetProoved = bytesNotYetProoved + (block.data.length - counter);
				}

				// System.out.println("Sending data msg: " + Tools.toASCIIString(line));

				// printLine(pacemateProgram.encode(line,(line.length -offset)));

				try {
					device.sendDataMessage(pacemateProgram.encode(line, (line.length - offset)));
				} catch (Exception e) {
					logDebug("Error while writing flash! Operation will be cancelled!");
					device.operationCancelled(this);
					return false;
				}

				linecounter++;
				if ((linecounter == 20) || (counter >= block.data.length)) {
					try {
						device.sendChecksum(pacemateProgram.crc);
					} catch (InvalidChecksumException e) {
						logDebug("Invalid Checksum - resend last part");
						// so resending the last 20 lines
						counter = counter - bytesNotYetProoved;
					} catch (Exception e) {
						logDebug("Error while writing flash! Operation will be cancelled!");
						device.operationCancelled(this);
						return false;
					}
					linecounter = 0;
					// System.out.println("CRC "+pacemateProgram.crc);
					pacemateProgram.crc = 0;
					bytesNotYetProoved = 0;
				}
			}

			try {
				// if block is completed copy data from RAM to Flash
				System.out.println("Prepare Flash and Copy Ram to Flash " + blockCount + " " + blockNumber + " "
						+ block.address);
				device.configureFlash(blockNumber, blockNumber);
				if (block.data.length > 1024)
					device.copyRAMToFlash(block.address, device.startAdressInRam, 4096);
				else if (block.data.length > 512)
					device.copyRAMToFlash(block.address, device.startAdressInRam, 1024);
				else if (block.data.length > 512)
					device.copyRAMToFlash(block.address, device.startAdressInRam, 512);
				else
					device.copyRAMToFlash(block.address, device.startAdressInRam, 256);
			} catch (Exception e) {
				logDebug("Error while copy RAM to Flash! Operation will be cancelled!");
				device.operationCancelled(this);
				return false;
			}

			// Notify listeners of the new status
			float progress = ((float) (blockCount - 2)) / ((float) program.getBlockCount());
			device.operationProgress(Operation.PROGRAM, progress);

			// Return with success if the user has requested to cancel this
			// operation
			if (isCancelled()) {
				logDebug("Operation has been cancelled");
				device.operationCancelled(this);
				return false;
			}

			blockCount++;
			if ((blockCount > 0) && (8 >= blockCount)) // Sektor 0-7 4kb
			{
				blockNumber++;
			} else if (blockCount == 16) // Sektor 8 32kb
			{
				blockNumber++;
			} else if (blockCount == 24) // Sektor 9 32kb
			{
				blockNumber++;
			} else if (blockCount == 32) // Sektor 10 32kb
			{
				blockNumber++;
			} else if (blockCount == 40) // Sektor 11 32kb
			{
				blockNumber++;
			} else if (blockCount == 48) // Sektor 12 32kb
			{
				blockNumber++;
			} else if (blockCount == 56) // Sektor 13 32kb
			{
				blockNumber++;
			} else if (blockCount == 64) // Sektor 14 32kb
			{
				blockNumber++;
			}
		}

		// Reboot (if requested by the user)
		if (rebootAfterFlashing) {
			logDebug("Rebooting device");
			device.reset();
		}
		return true;
	}

	/**
	 * @param line
	 */
	public void printLine(byte[] line) {
		for (int i = 0; i < line.length; i++)
			System.out.print(line[i]);
		System.out.println("");
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
