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

package de.uniluebeck.itm.wsn.devicedrivers.nulldevice;

import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.FlashType;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.Sectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dp
 */
public class NullDevice extends iSenseDeviceImpl {

	/**
	 *
	 */
	public NullDevice() {
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public boolean enterProgrammingMode() throws Exception {
		logWarn("No device connection available (Ignoring action)");
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void eraseFlash() throws Exception {
		logWarn("No device connection available (Ignoring action)");
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public ChipType getChipType() throws Exception {
		logWarn("No device connection available (Ignoring action)");
		return ChipType.Unknown;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public FlashType getFlashType() throws Exception {
		logWarn("No device connection available (Ignoring action)");
		return FlashType.Unknown;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public Operation getOperation() {
		logWarn("No device connection available (Ignoring action)");
		return Operation.NONE;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void leaveProgrammingMode() throws Exception {
		logWarn("No device connection available (Ignoring action)");
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public byte[] readFlash(int address, int len) throws Exception {
		logWarn("No device connection available (Ignoring action)");
		return new byte[]{};
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public boolean reset() throws Exception {
		logWarn("No device connection available (Ignoring action)");
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void send(MessagePacket p) throws Exception {
		logWarn("No device connection available (Ignoring action)");
	}

	@Override
	public void eraseFlash(Sectors.SectorIndex sector) throws Exception {
		logWarn("No device connection available (Ignoring action)");
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void shutdown() {
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void triggerGetMacAddress(boolean rebootAfterFlashing) throws Exception {
		logWarn("No device connection available (Ignoring action)");
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public boolean triggerProgram(IDeviceBinFile program, boolean rebootAfterFlashing) throws Exception {
		logWarn("No device connection available (Ignoring action)");
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void triggerSetMacAddress(MacAddress mac, boolean rebootAfterFlashing) throws Exception {
		logWarn("No device connection available (Ignoring action)");
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public byte[] writeFlash(int address, byte[] bytes, int offset, int len) throws Exception {
		logWarn("No device connection available (Ignoring action)");
		return new byte[]{};
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public boolean triggerReboot() throws Exception {
		logWarn("No device connection available (Ignoring action)");
		return false;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public String toString() {
		return "NullDevice";
	}

	@Override
	public int[] getChannels() {
		int[] channels = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
		return channels;
	}

	@Override
	public IDeviceBinFile loadBinFile(String fileName) {
		logWarn("No device connection available (Ignoring action)");
		return new NullBinFile();
	}

}
