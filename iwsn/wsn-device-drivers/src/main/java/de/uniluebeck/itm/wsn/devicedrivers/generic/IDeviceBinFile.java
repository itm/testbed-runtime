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


/**
 * Abstract base class for all binary file objects.
 *
 * @author Friedemann Wesner
 */
public interface IDeviceBinFile {

	/**
	 * Return the platform type that this bin file is to be used with.
	 *
	 * @return the platform type
	 */
	public ChipType getFileType();

	/**
	 * Check if the specified device type is compatible with using this bin file.
	 *
	 * @param deviceType
	 * @return true if this bin file is compatible to deviceType, false otherwise
	 */
	public boolean isCompatible(ChipType deviceType);

	/**
	 * Reset the block iterator used by method getNextBlock(). Afterwards, data blocks
	 * for writing to flash memory can be obtained successively by calling getNextBlock()
	 */
	public void resetBlockIterator();

	/**
	 * Check if there is still at least one data block that will be returned by
	 * the block iterator when calling getNextBlock(). There is no block that can be provided
	 * if the end of file has been reached.
	 *
	 * @return true if there is a block that will be returned by getNextBlock(), false otherwise
	 */
	public abstract boolean hasNextBlock();

	/**
	 * Get a data block of the bin file for writing it to flash memory along with the address within
	 * flash memory that the block should be written to.
	 * The method will iterate over all data of the binary file, starting at the beginning
	 * if resetBlockIterator() was called before. The size of a data block depends on the type
	 * of bin file that provides the data.
	 *
	 * @return the next pair of data block and block address, or null if end of file was reached
	 */
	public BinFileDataBlock getNextBlock();

	/**
	 * Calculate total number of blocks that make up the binary file.
	 *
	 * @return number of blocks
	 */
	public int getBlockCount();

	/**
	 * @return
	 */
	public int getLength();

}
