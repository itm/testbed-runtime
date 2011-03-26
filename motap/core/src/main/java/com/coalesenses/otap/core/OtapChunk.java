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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.TreeSet;

/**
 * @author Dennis Pfisterer
 */
public class OtapChunk implements Comparable<OtapChunk> {

	/**
	 *
	 */
	private static final Logger log = LoggerFactory.getLogger(OtapChunk.class);

	/**
	 *
	 */
	private short chunkNumber = 0;

	/**
	 *
	 */
	private short chunkIndex = 0;

	/**
	 *
	 */
	private TreeSet<OtapPacket> packets = new TreeSet<OtapPacket>();

	/**
	 *
	 */
	OtapChunk(short chunkNumber) {
		this.chunkNumber = chunkNumber;
	}

	/**
	 *
	 */
	public int compareTo(OtapChunk other) {
		if (other == null) {
			return 1;
		}

		if (other == this) {
			return 0;
		}

		return this.getChunkNumber() - other.getChunkNumber();
	}

	/**
	 *
	 */
	public boolean addPacket(OtapPacket p) {
		if (packets.contains(p)) {
			log.warn("Skipping packet " + p);
			return false;
		}

		p.setChunkNumber(this.chunkNumber);
		p.setIndex(chunkIndex++);
		packets.add(p);

		return true;
	}

	/**
	 *
	 */
	public OtapPacket getPacketByIndex(int index) {
		for (OtapPacket p : packets) {
			if (p.getIndex() == index) {
				return p;
			}
		}
		return null;
	}

	/**
	 *
	 */
	public short getChunkNumber() {
		return chunkNumber;
	}

	/**
	 *
	 */
	public Collection<OtapPacket> getPackets() {
		return packets;
	}

	/**
	 *
	 */
	public int getPacketCount() {
		return packets.size();
	}

}
