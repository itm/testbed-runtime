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
import de.uniluebeck.itm.wsn.devicedrivers.exceptions.FileLoadException;
import de.uniluebeck.itm.wsn.devicedrivers.generic.BinFileDataBlock;
import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import de.uniluebeck.itm.wsn.devicedrivers.generic.IDeviceBinFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author dp
 */
public class JennicBinFile implements IDeviceBinFile {

	private static final Logger log = LoggerFactory.getLogger(JennicBinFile.class);

	private final int blockSize = 128;

	private int blockIterator = 0;

	private byte[] bytes = null;

	private int length = -1;

	private String description;

	public JennicBinFile(byte[] bytes, String description) {
		this.bytes = bytes;
		this.length = bytes.length;
		this.description = description;
	}

	/**
	 * Constructor
	 *
	 * @param binFile
	 *
	 * @throws FileLoadException
	 */
	public JennicBinFile(File binFile) throws Exception {

		this.description = binFile.getAbsolutePath();

		if (!binFile.exists() || !binFile.canRead()) {
			throw new Exception("Unable to open file: " + binFile.getAbsolutePath());
		}

		try {
			bytes = new byte[(int) binFile.length()];
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(binFile));
			length = bis.read(bytes, 0, bytes.length);

			// log.debug("Read " + length + " bytes from " + binFile.getAbsolutePath());
			// log.debug("Total of blocks in " + binFile.getName() + ": " + getBlockCount());
			// int displayBytes = 200;
			// log.debug("Bin File starts with: " + Tools.toHexString(bytes, 0, bytes.length < displayBytes ?
			// bytes.length : displayBytes));

		} catch (Exception e) {
			log.error("Unable to load file[" + binFile + "]: " + e, e);
			throw new Exception("Unable to load file: " + binFile);
		}

	}

	/**
	 * Constructor
	 *
	 * @param filename
	 *
	 * @throws FileLoadException
	 */
	public JennicBinFile(String filename) throws Exception {
		this(new File(filename));
	}

	/**
	 * Calculate number of blocks to write
	 */
	private int getFullBlocksCount() {
		return (int) (length / blockSize);
	}

	/**
	 * Calculate residue after last block
	 */
	private int getResidue() {
		return (int) (length % blockSize == 0 ? 0 : length - (getFullBlocksCount() * blockSize));
	}

	private int getBlockOffset(int block) {
		int maxBlocks = getBlockCount();

		if (block >= maxBlocks) {
			log.error("Block number too large (requested " + block + "/ max" + maxBlocks + ")");
			return -1;
		}

		return block * blockSize;
	}

	private byte[] getBlock(int block) {
		int maxBlocks = getBlockCount();

		if (block >= maxBlocks) {
			log.error("Block number too large (requested " + block + " / maxNo " + maxBlocks + ")");
			return null;
		}

		int offset = getBlockOffset(block);
		int length = (getResidue() != 0 && block == maxBlocks - 1) ? getResidue() : blockSize;
		byte b[] = new byte[length];
		// log.debug("Returning block #" + block + " (" + length + " bytes at position " + offset);
		System.arraycopy(bytes, offset, b, 0, length);
		return b;
	}

	private boolean hasRepeatedPattern(byte b[], int offset, int repeat, byte pattern) {

		for (int i = 0; i < repeat; ++i) {
			if (b[offset + i] != pattern) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Insert flash header of a jennic device into bin file.
	 *
	 * @param b
	 *
	 * @return
	 */
	public boolean insertHeader(byte[] b) {
		ChipType chipType = getFileType();
		int headerStart = ChipType.getHeaderStart(chipType);
		int headerLength = ChipType.getHeaderLength(chipType);

		if (checkForID(b) == false) {
			if (log.isErrorEnabled()) {
				log.error("Empty chip ID: {}", StringUtils.toHexString(b));
			}
			throw new RuntimeException("Could not read MAC address of sensor node. Read value: " + StringUtils.toHexString(b));
		}

		if (headerStart >= 0 && headerLength > 0) {
			log.debug("Writing header for chip type {}: {} bytes @ header={}", new Object[]{
					chipType, headerLength, headerStart, StringUtils.toHexString(b)
			}
			);
			insertAt(headerStart, headerLength, b);
			return true;
		}

		log.error("Unknown chip type of file " + description);
		return false;
	}

	private boolean checkForID(byte[] b) {
		for (int i = 0; i < b.length; i++) {
			if (b[i] != -1) {
				return true;
			}
		}
		return false;
	}

	private void insertAt(int address, int len, byte[] b) {
		System.arraycopy(b, 0, bytes, address, len);
	}

	public ChipType getFileType() {
		if (bytes[0] == (byte) 0xE1) {
			log.debug("File type is JN5121");
			return ChipType.JN5121;

		} else if (hasRepeatedPattern(bytes, 0, 4, (byte) 0xE0)) {
			// log.debug("Start matches 4 x 0xE0 -> Could be JN513XR1 or JN513XR1");

			// JN513XR1
			{
				// OAD
				int start = 0x24, count = 8;
				boolean ok = hasRepeatedPattern(bytes, start, count, (byte) 0xFF);
				start += count;
				count = 4;
				ok = ok && hasRepeatedPattern(bytes, start, count, (byte) 0xF0);

				if (ok) {
					// log.debug("OAD Section found (8 x 0xFF, 4 x 0xF0)");
				} else {
					// log.debug("No OAD Section found -> not a JN513XR1");
				}

				// MAC Adress
				if (ok) {
					start += count;
					count = 32;
					ok = ok && hasRepeatedPattern(bytes, start, count, (byte) 0xFF);

					if (ok) {
						// log.debug("MAC Section found (32 x 0xFF)");
						log.debug("File type is JN513XR1");
						return ChipType.JN513XR1;
					}

				}

			}

			// JN513X
			{
				// MAC
				int start = 0x24, count = 32;
				boolean ok = hasRepeatedPattern(bytes, start, count, (byte) 0xFF);

				if (ok) {
					log.debug("File type is JN513X");
					return ChipType.JN513X;
				}
			}

		}
		else if ((bytes[0] == 0x00) && (bytes[1]==0) && (bytes[2]==(byte)0xe0)  && (bytes[3]==(byte)0xe0))
		{
			log.debug("File type is JN5148");
			return ChipType.JN5148;
		}
		log.error("Chip type of file " + description + " is UNKNOWN");
		return ChipType.Unknown;
	}

	public int getLength() {
		return length;
	}

	public BinFileDataBlock getNextBlock() {
		if (hasNextBlock()) {
			int offset = getBlockOffset(blockIterator);
			byte[] data = getBlock(blockIterator);

			blockIterator++;

			return new BinFileDataBlock(offset, data);
		} else {
			return null;
		}
	}

	@Override
	public void resetBlockIterator() {
		blockIterator = 0;
	}

	@Override
	public boolean hasNextBlock() {
		if (blockIterator < getBlockCount()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "JennicBinFile{" +
				"blockSize=" + blockSize +
				", blockIterator=" + blockIterator +
				", bytes=" + bytes +
				", length=" + length +
				", description='" + description + '\'' +
				'}';
	}

	public boolean isCompatible(ChipType deviceType) {
		return deviceType.equals(getFileType());
	}

	public int getBlockCount() {
		int b = getFullBlocksCount();

		if (getResidue() > 0) {
			b++;
		}

		return b;
	}

}