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


import de.uniluebeck.itm.wsn.devicedrivers.generic.BinFileDataBlock;
import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import de.uniluebeck.itm.wsn.devicedrivers.generic.IDeviceBinFile;

/**
 * @author Friedemann Wesner
 *         <p/>
 *         A dummy bin file
 */
public class NullBinFile implements IDeviceBinFile {

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#getBlockCount()
	 */

	@Override
	public int getBlockCount() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#getFileType()
	 */

	@Override
	public ChipType getFileType() {
		return ChipType.Unknown;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#getLength()
	 */

	@Override
	public int getLength() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#getNextBlock()
	 */

	@Override
	public BinFileDataBlock getNextBlock() {
		return new BinFileDataBlock(0, new byte[0]);
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#hasNextBlock()
	 */

	@Override
	public boolean hasNextBlock() {
		return false;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#isCompatible(ishell.device.ChipType)
	 */

	@Override
	public boolean isCompatible(ChipType deviceType) {
		return false;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#resetBlockIterator()
	 */

	@Override
	public void resetBlockIterator() {
	}

	@Override
	public String toString() {
		return "NullBinFile";
	}

}
