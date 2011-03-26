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

import com.google.common.base.Preconditions;
import de.uniluebeck.itm.tr.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author dp
 */

public class MessagePacket implements Message {

	/** */
	protected static Logger log = LoggerFactory.getLogger(MessagePacket.class);

	/** */
	private static long nextIdCounter = 0;

	/**
	 * Special character
	 */
	public static final byte STX = 0x02;

	/**
	 * Special character
	 */
	public static final byte ETX = 0x03;

	/**
	 * Special character
	 */
	public static final byte DLE = 0x10;

	/**
	 *
	 */
	public static final byte CR = 0x0D;

	/**
	 *
	 */
	public static final byte LF = 0x0A;

	/** */
	private byte[] content;

	/** */
	private int type = -1;

	/** */
	private long id = nextId();

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	protected MessagePacket() {
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public MessagePacket(int type, byte[] content) {
		setType(type);
		setContent(content);
	}

	public MessagePacket(final byte[] messageBytes) {
		Preconditions.checkArgument(messageBytes.length > 0, "A MessagePacket must contain at least one byte that "
				+ "determines the message type!");
		setType(messageBytes[0]);
		if (messageBytes.length > 1) {
			byte[] contentBytes = new byte[messageBytes.length-1];
			System.arraycopy(messageBytes, 1, contentBytes, 0, messageBytes.length-1);
			setContent(contentBytes);
		} else {
			setContent(new byte[]{});
		}
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	private synchronized static long nextId() {
		if (nextIdCounter >= Long.MAX_VALUE - 1) {
			nextIdCounter = 0;
		}

		return ++nextIdCounter;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public static MessagePacket parse(byte[] buffer, int offset, int length) {
		MessagePacket p = new MessagePacket();

		// Determine message type
		p.type = 0xFF & ((int) buffer[offset]);

		// Extract message content
		p.content = new byte[length - 1];
		System.arraycopy(buffer, offset + 1, p.content, 0, length - 1);

		return p;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MessagePacket[id=");
		builder.append(id);
		builder.append(",type=");
		builder.append(type);
		builder.append(",content(hex)=[");
		builder.append(StringUtils.toHexString(content));
		builder.append("],content(string)=\"");
		if (type == PacketTypes.LOG && content.length > 1) {
			builder.append(content[0] == PacketTypes.LogType.DEBUG ?
					"DEBUG: " + new String(content, 1, content.length-1) :
					"FATAL: " + new String(content, 1, content.length-1)
			);
		} else {
			builder.append(new String(content));
		}
		builder.append("\"]");
		return builder.toString();
	}

	public byte[] getContent() {
		return content;
	}

	/**
	 * Sets the given content
	 *
	 * @param content
	 */
	public void setContent(byte[] content) {
		this.content = new byte[content.length];
		System.arraycopy(content, 0, this.content, 0, content.length);
	}

	/**
	 * Returns the current type
	 *
	 * @return
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the type
	 *
	 * @param type
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Returns the id
	 *
	 * @return
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns a copy of the byte-array representation of this packet: TYPE CONTENT[0] ... CONTENT[CONTENT.length-1]
	 *
	 * @return a copy of the byte-array representation of this packet.
	 */
	public byte[] getByteArray() {
		byte[] bytes = new byte[1 + content.length];
		bytes[0] = (byte) (0xFF & type);
		System.arraycopy(content, 0, bytes, 1, content.length);
		return bytes;
	}

}
