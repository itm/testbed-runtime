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

import de.uniluebeck.itm.wsn.devicedrivers.exceptions.FileLoadException;
import de.uniluebeck.itm.wsn.devicedrivers.generic.BinFileDataBlock;
import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import de.uniluebeck.itm.wsn.devicedrivers.generic.IDeviceBinFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Binary file used to program a Telos B device. The file is assumed to be in intel hex format.
 *
 * @author Friedemann Wesner
 */
public class TelosbBinFile implements IDeviceBinFile {

	private static final Logger log = LoggerFactory.getLogger(TelosbBinFile.class);

	private final int maxBlockSize = 240 - 16;

	private BlockIterator blockIterator = new BlockIterator();

	private List<Segment> segments = new ArrayList<Segment>();

	private String description;

	public TelosbBinFile(byte[] binaryData, String description) throws Exception {
		this.description = description;
		reload(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(binaryData))));
	}

	/**
	 * Constructor
	 *
	 * @param file
	 * @throws Exception
	 */
	public TelosbBinFile(File file) throws Exception {
		this.description = file.getAbsolutePath();
		if (file == null) {
			throw new FileLoadException();
		}
		reload(new BufferedReader(new FileReader(file)));
	}

	/**
	 * Constructor
	 *
	 * @param fileName
	 * @throws FileLoadException
	 */
	public TelosbBinFile(String fileName) throws Exception {
		this(new File(fileName));
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#getBlockCount()
	 */

	@Override
	public int getBlockCount() {
		int blocksPerSegment = 0;
		int totalBlocks = 0;

		for (Segment seg : segments) {
			blocksPerSegment = 0;
			while ((blocksPerSegment * maxBlockSize) < seg.data.length) {
				blocksPerSegment++;
			}
			totalBlocks += blocksPerSegment;
		}
		return totalBlocks;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#getFileType()
	 */

	@Override
	public ChipType getFileType() {
		return ChipType.TelosB;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#getLength()
	 */

	@Override
	public int getLength() {
		int totalLength = 0;

		for (Segment seg : segments) {
			totalLength += seg.data.length;
		}

		return totalLength;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#getNextBlock()
	 */

	@Override
	public BinFileDataBlock getNextBlock() {
		BinFileDataBlock dataBlock = null;
		int actualBlockSize = maxBlockSize;
		byte[] data = null;
		Segment seg = null;

		if (hasNextBlock()) {
			seg = segments.get(blockIterator.segmentNo);
			if (blockIterator.byteNo + actualBlockSize > seg.data.length) {
				actualBlockSize = seg.data.length - blockIterator.byteNo;
			}
			data = new byte[actualBlockSize];
			System.arraycopy(seg.data, blockIterator.byteNo, data, 0, actualBlockSize);
			dataBlock = new BinFileDataBlock(seg.startAddress + blockIterator.byteNo, data);

			if (blockIterator.byteNo + maxBlockSize >= seg.data.length) {
				blockIterator.segmentNo++;
				blockIterator.byteNo = 0;
			} else {
				blockIterator.byteNo += actualBlockSize;
			}
		}

		return dataBlock;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#hasNextBlock()
	 */

	@Override
	public boolean hasNextBlock() {
		if (blockIterator.segmentNo >= segments.size()) {
			return false;
		}
		if (blockIterator.byteNo >= segments.get(blockIterator.segmentNo).data.length) {
			return false;
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#isCompatible(ishell.device.ChipType)
	 */

	@Override
	public boolean isCompatible(ChipType deviceType) {
		return deviceType.equals(getFileType());
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#reload()
	 */

	private void reload(BufferedReader reader) throws Exception {
		String line = null;
		StringTokenizer sTokenizer = null;
		int lineAddress = 0;
		int currentAddress = 0;
		int segmentStartAddress = 0;
		int dataLength = 0;
		int dataType = 0;
		byte segmentData[] = new byte[0];
		byte tempData[];

		try {
			segments.clear();
			while ((line = reader.readLine()) != null) {
				// check for correct ihex format
				if (line.charAt(0) != ':') {
					log.error("File is not in correct intel hex format.");
					throw new FileLoadException();
				}

				// remove spaces
				sTokenizer = new StringTokenizer(line, " ", false);
				line = "";
				while (sTokenizer.hasMoreElements()) {
					line += sTokenizer.nextElement();
				}

				// read data segments
				dataLength = Integer.parseInt(line.substring(1, 3), 16);
				lineAddress = Integer.parseInt(line.substring(3, 7), 16);
				dataType = Integer.parseInt(line.substring(7, 9), 16);

				if (dataType == 0x00) {
					if (currentAddress != lineAddress) {
						if (segmentData.length > 0) {
							segments.add(new Segment(segmentStartAddress, segmentData));
						}
						currentAddress = lineAddress;
						segmentStartAddress = lineAddress;
						segmentData = new byte[0];
					}
					tempData = new byte[segmentData.length + dataLength];
					System.arraycopy(segmentData, 0, tempData, 0, segmentData.length);
					for (int i = 0; i < dataLength; i++) {
						tempData[segmentData.length + i] =
								(byte) Integer.parseInt(line.substring(9 + 2 * i, 9 + 2 * i + 2), 16);
					}
					segmentData = tempData;
					currentAddress += tempData.length;
				}
			}
			if (segmentData.length > 0) {
				segments.add(new Segment(segmentStartAddress, segmentData));
			}
		} catch (Exception e) {
			log.error("Unable to load file: " + e, e);
			throw new FileLoadException();
		}
	}

	/* (non-Javadoc)
	 * @see ishell.device.IDeviceBinFile#resetBlockIterator()
	 */

	@Override
	public void resetBlockIterator() {
		blockIterator.reset();
	}

	@Override
	public String toString() {
		return "TelosbBinFile{" +
				"maxBlockSize=" + maxBlockSize +
				", blockIterator=" + blockIterator +
				", segments=" + segments +
				'}';
	}

	/*
		 * A continued data segment of the ihex file that was read
		 */

	private class Segment {

		public int startAddress;

		public byte[] data;

		public Segment(int startAddress, byte[] data) {
			this.startAddress = startAddress;
			this.data = data;
		}
	}

	private class BlockIterator {

		int segmentNo = 0;

		int byteNo = 0;

		public void reset() {
			segmentNo = 0;
			byteNo = 0;
		}
	}
}
