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

package com.coalesenses.otap.core.seraerial;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.PacketTypes;

//	-------------------------------------------------------------------------

/**
 *
 */
public class SerialRoutingPacket extends SerAerialPacket {

	/** */
	private int routing;

	/** */
	private int reserved;


	//	-------------------------------------------------------------------------

	/**
	 *
	 */
	public SerialRoutingPacket() {
		super();
	}

	//	-------------------------------------------------------------------------

	/**
	 *
	 */
	public SerialRoutingPacket(byte[] content, int routing, int reserved) {
		super(content);
		this.routing = routing;
		this.reserved = reserved;
	}

	//	-------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append("src:" + src + " dest: " + dest + " lqi: " + lqi + " iface: " + iface + " options: " + opts +
				" routing " + routing + " reserved " + reserved + " len: " + len + " content: "
				+ StringUtils.toHexString(content)
		);
		return s.toString();
	}

	//	-------------------------------------------------------------------------
	/**
	 *
	 */
	/*
	public void parse(MessagePacket p) {
		byte[] s = p.getContent();

		if (s == null || s.length < 1) {
			log.warn("Packet[" + p.getId() + "] length less than 1. This must be an invalid SerialRoutingPacket");
			return;
		} else if (s[0] == 0x00 && s.length < 9) {
			log.warn("Packet[" + p.getId() + "] length less than 9. This must be an invalid SerialRoutingPacket");
			return;
		} else if (s[0] == 0x01 && s.length < 3) {
			log.warn("Packet[" + p.getId() + "] length less than 3. This must be an invalid SerialRoutingPacket");
			return;
		}

		if (s[0] == 0x00) {
			packetType = PacketType.Packet;
			src = ((0xFF & s[1]) << 8) + (0xFF & s[2]);
			dest = ((0xFF & s[3]) << 8) + (0xFF & s[4]);
			lqi = ((0xFF & s[5]) << 8) + (0xFF & s[6]);
			iface = (0xFF & s[7]);
			len = (0xFF & s[8]);

			if (len > 0) {
				if (s.length >= 9 + len) {
					this.content = new byte[len];
					System.arraycopy(s, 9, this.content, 0, len);
				} else
					log.warn("Packet[" + p.getId() + "]: Advertised payload lenght: " + len + ", Complete packet size is " + s.length
							+ ", max payload len: " + (s.length - 9));
			}

		} else if (s[0] == 0x01) {
			packetType = PacketType.Confirm;
			state = s[1];
			tries = s[2];
		} else {
			log.warn("Packet[" + p.getId() + "]: Unexpected packet type [" + Tools.toHexString(s[0]) + "]");
		}

	}*/

	//	-------------------------------------------------------------------------

	/**
	 *
	 */
	public byte[] toByteArray() {
		byte tmp[] = new byte[7 + this.content.length];
		tmp[0] = PacketTypes.ISenseCommands.ISENSE_ISI_COMMAND_ISHELL_TO_ROUTING;
		tmp[1] = (byte) ((dest >> 8) & 0xFF);
		tmp[2] = (byte) (dest & 0xFF);
		tmp[3] = (byte) (0xFF & opts);
		tmp[4] = (byte) (0xFF & routing);
		tmp[5] = (byte) (0xFF & reserved);
		tmp[6] = (byte) (0xFF & len);

		System.arraycopy(this.content, 0, tmp, 7, this.content.length);

		//log.debug("SerialRoutingPacket: Header: " + Tools.toHexString(tmp, 0, 9) + ", Content: "+ Tools.toHexString(tmp, 9));

		return tmp;
	}

	//	-------------------------------------------------------------------------
	// Getter and setters
	//	-------------------------------------------------------------------------


	public int getRouting() {
		return routing;
	}

	public void setRouting(int routing) {
		this.routing = routing;
	}

	public int getReserved() {
		return reserved;
	}

	public void setReserved(int reserved) {
		this.reserved = reserved;
	}


}