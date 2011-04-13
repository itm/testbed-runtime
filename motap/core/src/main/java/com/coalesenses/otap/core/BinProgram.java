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

package com.coalesenses.otap.core;


import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.ChipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.TreeSet;

public class BinProgram {

	public enum OtapFunctionality {

		Nothing, Otap, Motap;
	}

	/**
	 *
	 */
	private static final Logger log = LoggerFactory.getLogger(BinProgram.class);

	/**
	 * Number of OtapPackets in one chunk
	 */
	private static int MAX_CHUNK_SIZE = 64;

	/**
	 * Number of payload bytes in one OtapPacket
	 */
	private static int MAX_PACKET_CODE_SIZE = 64;

	/**
	 *
	 */
	private TreeSet<OtapChunk> chunks = new TreeSet<OtapChunk>();

	/**
	 *
	 */
	private ChipType chipType = ChipType.Unknown;

	/**
	 *
	 */
	public OtapFunctionality otapFunctionality = OtapFunctionality.Nothing;

	/**
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	//public BinProgram(File binfile, int revisionNumber) throws FileNotFoundException, IOException {
	public BinProgram(File binfile) throws FileNotFoundException, IOException {

		//this(new FileInputStream(binfile), revisionNumber);
		this(new FileInputStream(binfile));


		// Chip type
		byte[] bytes = new byte[(int) binfile.length()];
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(binfile));
		int length = bis.read(bytes, 0, bytes.length);

		chipType = getFileType(bytes);

		otapFunctionality = testOtapFunctionality(bytes);

		log.info(
				"OTAP::Loaded OTAP bin file: " + binfile + " Chip type: " + chipType + " OtapFunctionality of loaded program: " + otapFunctionality
		);

	}

	/**
	 *
	 */
	private OtapFunctionality testOtapFunctionality(byte[] bytes) {
		byte[] motap_identifier = {'M', 'O', 'T', 'A', 'P', 'O', 'K'};
		//log.info(Tools.toString(id));

		if (StringUtils.toHexString(bytes).contains(StringUtils.toHexString(motap_identifier))) {
			return OtapFunctionality.Motap;
		} else {
			byte[] otap_identifier = {'O', 'T', 'A', 'P', 'O', 'K'};
			if (StringUtils.toHexString(bytes).contains(StringUtils.toHexString(otap_identifier))) {
				return OtapFunctionality.Otap;
			}
		}
		return OtapFunctionality.Nothing;
	}

	/**
	 * @throws IOException
	 */
	//public BinProgram(InputStream binfile, int revisionNumber) throws IOException {
	public BinProgram(InputStream binfile) throws IOException {
		loadBinFile(binfile);
	}

	/**
	 * @throws IOException
	 */
	private void loadBinFile(InputStream binfile) throws IOException {
		//log.debug("Loading sofware revision " + this.revisionNumber);

		BufferedInputStream in = new BufferedInputStream(binfile);
		byte[] packet = new byte[MAX_PACKET_CODE_SIZE];
		int packetLen = -1;
		int address = 0;
		OtapChunk currentChunk = new OtapChunk((short) 0);

		chunks.clear();
		chunks.add(currentChunk);

		while ((packetLen = in.read(packet, 0, MAX_PACKET_CODE_SIZE)) > 0) {

			//Check if the current chunk is full
			if (currentChunk.getPacketCount() >= MAX_CHUNK_SIZE) {
				//log.debug("Current chunk #" + currentChunk.getChunkNumber() + " full (" + currentChunk.getPacketCount()+ " packets), creating new one");
				currentChunk = new OtapChunk((short) (currentChunk.getChunkNumber() + 1));
				chunks.add(currentChunk);
			}

			//Create the next packet
			OtapPacket p = new OtapPacket(address, packet, 0, packetLen);
			currentChunk.addPacket(p);
			log.debug("Added packet: " + p);

			address += packetLen;
		}

		log.info("OTAP::Done, got " + chunks.size() + " chunks.");
	}

	/**
	 *
	 */
	public OtapChunk getChunk(int number) {
		for (OtapChunk c : chunks) {
			if (c.getChunkNumber() == number) {
				return c;
			}
		}

		return null;
	}

	/**
	 *
	 */
	public int getPacketCount() {
		int p = 0;
		for (OtapChunk c : chunks) {
			p += c.getPacketCount();
		}
		return p;
	}

	/**
	 *
	 */
	public int getPacketCount(int minChunkIncl, int maxChunkIncl) {
		int p = 0;

		if (minChunkIncl > maxChunkIncl) {
			return 0;
		}

		for (int i = minChunkIncl; i <= maxChunkIncl; ++i) {
			OtapChunk c = getChunk(i);
			if (c == null) {
				continue;
			}
			p += c.getPacketCount();
		}

		return p;
	}

	/**
	 *
	 */
	public short getChunkCount() {
		return (short) chunks.size();
	}

	//	-------------------------------------------------------------------------
	/**
	 *
	 */
	/*public int getRevisionNumber() {
		return revisionNumber;
	}
	*/

	/**
	 *
	 */
	public ChipType getFileType(byte[] bytes) {

		if (bytes[0] == (byte) 0xE1) {
			log.debug("Chip type is JN5121");
			return ChipType.JN5121;

		} else if (hasRepeatedPattern(bytes, 0, 4, (byte) 0xE0)) {
			log.debug("Start matches 4 x 0xE0 -> Could be JN513XR1 or JN513XR1");

			//JN513XR1
			{
				//OAD
				int start = 0x24, count = 8;
				boolean ok = hasRepeatedPattern(bytes, start, count, (byte) 0xFF);
				start += count;
				count = 4;
				ok = ok && hasRepeatedPattern(bytes, start, count, (byte) 0xF0);

				if (ok) {
					log.debug("OAD Section found (8 x 0xFF, 4 x 0xF0)");
				} else {
					log.debug("No OAD Section found -> not a JN513XR1");
				}

				//MAC Adress
				if (ok) {
					start += count;
					count = 32;
					ok = ok && hasRepeatedPattern(bytes, start, count, (byte) 0xFF);

					if (ok) {
						log.debug("MAC Section found (32 x 0xFF)");
						log.debug("Chip type is JN513XR1");
						return ChipType.JN513XR1;
					}
				}
			}
			//JN513X
			{
				//MAC
				int start = 0x24, count = 32;
				boolean ok = hasRepeatedPattern(bytes, start, count, (byte) 0xFF);

				if (ok) {
					log.debug("Chip type is JN513X");
					return ChipType.JN513X;
				}
			}
		}
		return ChipType.Unknown;
	}

	/**
	 *
	 */
	private boolean hasRepeatedPattern(byte b[], int offset, int repeat, byte pattern) {

		for (int i = 0; i < repeat; ++i) {
			if (b[offset + i] != pattern) {
				return false;
			}
		}

		return true;
	}

	/**
	 *
	 */
	public ChipType getChipType() {
		return chipType;
	}

	/**
	 *
	 */
	public OtapFunctionality getOtapFunctionality() {
		return otapFunctionality;
	}

}
