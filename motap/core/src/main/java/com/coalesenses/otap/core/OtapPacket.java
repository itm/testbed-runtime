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


import de.uniluebeck.itm.tr.util.TimeDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Pfisterer
 */
public class OtapPacket implements Comparable<OtapPacket> {

	/**
	 *
	 */
	private static final Logger log = LoggerFactory.getLogger(OtapPacket.class);

	/**
	 *
	 */
	private byte[] content = null;

	/**
	 *
	 */
	private int chunkNumber = 0;

	/**
	 *
	 */
	private int overallPacketNumber = 0;

	/**
	 *
	 */
	private int index = 0;

	/**
	 *
	 */
	private TimeDiff transmissionTime = new TimeDiff();

	/**
	 * @param startAddress
	 * @param content
	 * @param offset
	 * @param length
	 */
	OtapPacket(int startAddress, byte[] content, int offset, int length) {
		this.overallPacketNumber = startAddress / 64;
		setContent(content, offset, length);
	}

	/**
	 * @param o
	 *
	 * @return
	 */
	public int compareTo(OtapPacket o) {

		if (o == null) {
			return 1;
		}

		if (o == this) {
			return 0;
		}

		return this.overallPacketNumber - o.overallPacketNumber;
	}

	@Override
	public String toString() {
		int bytes = content != null ? content.length : 0;

		return "Packet: chunkNumber " + chunkNumber + ", overallPacketNumber: " + overallPacketNumber + ", index " + index + ", " + bytes + " bytes";
	}

	/**
	 *
	 */
	public void setTransmissionTime() {
		this.transmissionTime.touch();
	}

	/**
	 * @return
	 */
	public TimeDiff getTransmissionTime() {
		return transmissionTime;
	}

	/**
	 * @return
	 */
	public int getChunkNumber() {
		return chunkNumber;
	}

	/**
	 * @param chunkNumber
	 */
	public void setChunkNumber(int chunkNumber) {
		this.chunkNumber = chunkNumber;
	}

	/**
	 * @return
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * @param content
	 * @param offset
	 * @param length
	 */
	public void setContent(byte[] content, int offset, int length) {
		this.content = new byte[length];
		System.arraycopy(content, offset, this.content, 0, length);
	}

	/**
	 * @return
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @param index
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @return
	 */
	public int getOverallPacketNumber() {
		return overallPacketNumber;
	}

	/**
	 * @param overallPacketNumber
	 */
	public void setOverallPacketNumber(int overallPacketNumber) {
		this.overallPacketNumber = overallPacketNumber;
	}

}
