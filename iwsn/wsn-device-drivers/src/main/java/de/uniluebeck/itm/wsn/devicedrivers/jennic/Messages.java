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

import java.nio.ByteBuffer;

/**
 * @author Markus Class defining specific message types
 */
public class Messages {
	/**	 */
	public static final int FLASH_ERASE_REQUEST = 0x07;

	/**	 */
	public static final int FLASH_ERASE_RESPONSE = 0x08;

	/**	 */
	public static final int FLASH_PROGRAM_REQUEST = 0x09;

	/**	 */
	public static final int FLASH_PROGRAM_RESPONSE = 0x0a;

	/**	 */
	public static final int FLASH_READ_REQUEST = 0x0b;

	/**	 */
	public static final int FLASH_READ_RESPONSE = 0x0c;

	/**	 */
	public static final int SECTOR_ERASE_REQUEST = 0x0d;

	/**	 */
	public static final int SECTOR_ERASE_RESPONSE = 0x0e;

	/**	 */
	public static final int WRITE_SR_REQUEST = 0x0f;

	/**	 */
	public static final int WRITE_SR_RESPONSE = 0x10;

	/**	 */
	public static final int RAM_WRITE_REQUEST = 0x1d;

	/**	 */
	public static final int RAM_WRITE_RESPONSE = 0x1e;

	/**	 */
	public static final int RAM_READ_REQUEST = 0x1f;

	/**	 */
	public static final int RAM_READ_RESPONSE = 0x20;

	/**	 */
	public static final int RUN_REQUEST = 0x21;

	/**	 */
	public static final int RUN_RESPONSE = 0x22;

	/**	 */
	public static final int FLASH_TYPE_READ_REQUEST = 0x25;

	/**	 */
	public static final int FLASH_TYPE_READ_RESPONSE = 0x26;

	/**	 */
	public static final int CHANGE_BAUD_RATE_REQUEST = 0x27;

	/**	 */
	public static final int CHANGE_BAUD_RATE_RESPONSE = 0x28;

	/**	 */
	public static final int FLASH_CONFIGURE_REQUEST = 0x2C;

	/**	 */
	public static final int FLASH_CONFIGURE_RESPONSE = 0x2D;
	
	/**	 */
	public static final int CHIP_ID_REQUEST = 0x32;
	
	/**	 */
	public static final int CHIP_ID_RESPONSE = 0x33;

	/**	 */
	public static byte[] addressToBytes(int value) {
		byte[] array = ByteBuffer.allocate(4).putInt(value).array();
		byte[] result = new byte[array.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = array[array.length - 1 - i];
		}
		return result;
	}

	/**	 */
	public static byte[] flashReadRequestMessage(int address, int length) {
		byte[] message = new byte[1 + 4 + 2];
		message[0] = FLASH_READ_REQUEST;
		System.arraycopy(addressToBytes(address), 0, message, 1, 4);
		System.arraycopy(addressToBytes(length), 0, message, 5, 2);
		return message;
	}

	/**	 */
	public static byte[] flashEraseRequestMessage() {
		return new byte[]{FLASH_ERASE_REQUEST};
	}

	/**	 */
	public static byte[] flashProgramRequestMessage(int address, byte[] data) {
		byte[] message = new byte[1 + 4 + data.length];
		message[0] = FLASH_PROGRAM_REQUEST;
		System.arraycopy(addressToBytes(address), 0, message, 1, 4);
		System.arraycopy(data, 0, message, 5, data.length);
		return message;
	}

	/**	 */
	public static byte[] sectorEraseRequestMessage(Sectors.SectorIndex index) {
		byte[] message = new byte[1 + 1];
		message[0] = SECTOR_ERASE_REQUEST;
		message[1] = (byte) (index.ordinal() & 0xFF);
		return message;
	}

	/**	 */
	public static byte calculateChecksum(byte[] message) {
		return calculateChecksum(message, 0, message.length);
	}

	/**	 */
	public static byte calculateChecksum(byte[] message, int start, int length) {
		byte checksum = 0;
		for (int i = start; i < length; ++i) {
			checksum ^= message[i];
		}
		return checksum;
	}

	/**	 */
	public static byte[] flashTypeReadRequestMessage() {
		return new byte[]{FLASH_TYPE_READ_REQUEST};
	}

	/**	 */
	public static byte[] ramReadRequestMessage(int address, int length) {
		byte[] message = new byte[1 + 4 + 2];
		message[0] = RAM_READ_REQUEST;
		System.arraycopy(addressToBytes(address), 0, message, 1, 4);
		System.arraycopy(addressToBytes(length), 0, message, 5, 2);
		return message;
	}

	/**	 */
	public static byte[] flashConfigureRequestMessage(FlashType flashType) {
		byte[] message = new byte[6];
		message[0] = FLASH_CONFIGURE_REQUEST;
		message[1] = flashType.getJennicId();
		return message;
	}

	/**	 */
	public static byte[] statusRegisterWriteMessage(byte status) {
		byte[] message = new byte[2];
		message[0] = WRITE_SR_REQUEST;
		message[1] = status;
		return message;
	}
	
	/** */
	public static byte[] changeBaudRateMessage() {
		byte[] message = new byte[2];
		message[0] = CHANGE_BAUD_RATE_REQUEST;
		message[1] = 9;
		return message;
	}
	
	/** */
	public static byte[] chipIdMessage() {
		return new byte[] { CHIP_ID_REQUEST };
	}

}
