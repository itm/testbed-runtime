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

package de.uniluebeck.itm.wsn.devicedrivers.pacemate;

import java.nio.ByteBuffer;

/**
 * @author Markus Class defining specific message types
 */
public class Messages {

	/**
	 * Pacemate Style
	 */
	public static final int AUTO_BAUD_REQUEST = 0x3F;

	/**
	 * Pacemate Style
	 */
	public static final int READ_ID_REQUEST = 0x4A;

	/**
	 * Pacemate Style
	 */
	public static final int UNLOCK_REQUEST = 0x55;

	/**
	 * Pacemate Style
	 */
	public static final int SET_ECHO_REQUEST = 0x41;

	/**
	 * Pacemate Style
	 */
	public static final int FLASH_ERASE_REQUEST = 0x45;

	/**
	 * Pacemate Style
	 */
	public static final int FLASH_CONFIGURE_REQUEST = 0x50;

	/**
	 * Pacemate Style
	 */
	public static final int WRITE_TO_RAM_REQUEST = 0x57;

	/**
	 * Pacemate Style
	 */
	public static final int COPY_RAM_TO_FLASH_REQUEST = 0x43;

	/**
	 * Pacemate Style
	 */
	public static final int FLASH_READ_REQUEST = 0x52;

	/**	 */
	public static final int FLASH_PROGRAM_REQUEST = 0x09;

	/**	 */
	public static final int FLASH_PROGRAM_RESPONSE = 0x0a;

	/**	 */
	public static final int FLASH_READ_RESPONSE = 0x0c;

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

	/**
	 public static final int FLASH_TYPE_READ_REQUEST = 0x25;

	 /**
	 public static final int FLASH_TYPE_READ_RESPONSE = 0x26;*/


	/**
	 * Pacemate Style
	 */
	public static final String CMD_SUCCESS = "0";

	/**
	 * Pacemate Style
	 */
	public static final String SYNCHRONIZED = "Synchronized";

	/**
	 * Pacemate Style
	 */
	public static final String SYNCHRONIZED_OK = "OK";

	/**
	 * Pacemate Style
	 */
	public static final String OK = "OK";

	/**
	 * Pacemate Style
	 */
	public static final String DATA_ECHO = "DATA_ECHO";

	/**
	 * Pacemate Style
	 */
	public static final String DATA = "DATA";

	/**
	 * Pacemate Style
	 */
	public static final String ECHO_RESPONSE = "A ";

	/**
	 * Pacemate Style
	 */
	public static final String INVALID_COMMAND = "1";

	/**
	 * Pacemate Style
	 */
	public static final String SRC_ADDR_ERROR = "2";

	/**
	 * Pacemate Style
	 */
	public static final String DST_ADDR_ERROR = "3";

	/**
	 * Pacemate Style
	 */
	public static final String SRC_ADDR_NOT_MAPPED = "4";

	/**
	 * Pacemate Style
	 */
	public static final String DST_ADDR_NOT_MAPPED = "5";

	/**
	 * Pacemate Style
	 */
	public static final String COUNT_ERROR = "6";

	/**
	 * Pacemate Style
	 */
	public static final String INVALID_SECTOR = "7";

	/**
	 * Pacemate Style
	 */
	public static final String SECTOR_NOT_BLANK = "8";

	/**
	 * Pacemate Style
	 */
	public static final String SECTOR_NOT_PREPARED_FOR_WRITE_OPERATION = "9";

	/**
	 * Pacemate Style
	 */
	public static final String COMPARE_ERROR = "10";

	/**
	 * Pacemate Style
	 */
	public static final String BUSY = "11";

	/**
	 * Pacemate Style
	 */
	public static final String PARAM_ERROR = "12";

	/**
	 * Pacemate Style
	 */
	public static final String ADDR_ERROR = "13";

	/**
	 * Pacemate Style
	 */
	public static final String ADDR_NOT_MAPPED = "14";

	/**
	 * Pacemate Style
	 */
	public static final String CMD_LOCKED = "15";

	/**
	 * Pacemate Style
	 */
	public static final String INVALID_CODE = "16";

	/**
	 * Pacemate Style
	 */
	public static final String INVALID_BAUD_RATE = "17";

	/**
	 * Pacemate Style
	 */
	public static final String INVALID_STOP_BIT = "18";

	/**
	 * Pacemate Style
	 */
	public static final String CODE_READ_PROTECTION_ENABLED = "19";

	/**
	 * Pacemate Style
	 */
	public static String getErrorMessage(int returnCodeInt) {
		switch (returnCodeInt) {
			case 0:
				return "CMD_SUCCESS";
			case 1:
				return "INVALID_COMMAND";
			case 2:
				return "SRC_ADDR_ERROR";
			case 3:
				return "DST_ADDR_ERROR";
			case 4:
				return "SRC_ADDR_NOT_MAPPED";
			case 5:
				return "DST_ADDR_NOT_MAPPED";
			case 6:
				return "COUNT_ERROR";
			case 7:
				return "INVALID_SECTOR";
			case 8:
				return "SECTOR_NOT_BLANK";
			case 9:
				return "SECTOR_NOT_PREPARED_FOR_WRITE_OPERATION";
			case 10:
				return "COMPARE_ERROR";
			case 11:
				return "BUSY";
			case 12:
				return "PARAM_ERROR";
			case 13:
				return "ADDR_ERROR";
			case 14:
				return "ADDR_NOT_MAPPED";
			case 15:
				return "CMD_LOCKED";
			case 16:
				return "INVALID_CODE";
			case 17:
				return "INVALID_BAUD_RATE";
			case 18:
				return "INVALID_STOP_BIT";
			case 19:
				return "CODE_READ_PROTECTION_ENABLED";
			case -1:
				return "";
			default:
				return "";
		}
	}

	/**	 */
	public static byte[] addressToBytes(int value) {
		byte[] array = ByteBuffer.allocate(4).putInt(value).array();
		byte[] result = new byte[array.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = array[array.length - 1 - i];
		}
		return result;
	}

	/**
	 * Pacemate style
	 */
	public static byte[] AutoBaudRequestMessage() {
		byte[] message = new byte[1];
		message[0] = AUTO_BAUD_REQUEST;
		return message;
	}

	/**
	 * Pacemate style
	 */
	public static byte[] AutoBaudRequest2Message() {
		byte[] message = new byte[12];
		message[0] = 0x53;
		message[1] = 0x79;
		message[2] = 0x6e;
		message[3] = 0x63;
		message[4] = 0x68;
		message[5] = 0x72;
		message[6] = 0x6f;
		message[7] = 0x6e;
		message[8] = 0x69;
		message[9] = 0x7a;
		message[10] = 0x65;
		message[11] = 0x64;
		return message;
	}

	/**
	 * Pacemate style
	 */
	public static byte[] AutoBaudRequest3Message() {
		byte[] message = new byte[5];
		message[0] = 0x31;
		message[1] = 0x34;
		message[2] = 0x37;
		message[3] = 0x34;
		message[4] = 0x35;
		return message;
	}

	/**
	 * Pacemate style
	 */
	public static byte[] SetEchoOffMessage() {
		byte[] message = new byte[3];
		message[0] = SET_ECHO_REQUEST;
		message[1] = 0x20; // = Leerzeichen
		message[2] = 0x30; // = 0
		return message;
	}

	/**
	 * Pacemate style
	 */
	public static byte[] SetEchoOnMessage() {
		byte[] message = new byte[2];
		message[0] = SET_ECHO_REQUEST;
		message[1] = 0x20; // = Leerzeichen
		message[1] = 0x31; // = 1
		return message;
	}

	/**
	 * Pacemate style
	 */
	public static byte[] ReadPartIDRequestMessage() {
		byte[] message = new byte[1];
		message[0] = READ_ID_REQUEST;
		return message;
	}

	/**
	 * Pacemate style
	 */
	public static byte[] Unlock_RequestMessage() {
		byte[] message = new byte[7];
		message[0] = UNLOCK_REQUEST;
		message[1] = 0x20; // = Leerzeichen
		message[2] = 0x32;
		message[3] = 0x33;
		message[4] = 0x31;
		message[5] = 0x33;
		message[6] = 0x30;
		return message;
	}

	/**
	 * Pacemate Style
	 */
	public static byte[] flashConfigureRequestMessage(int startSektor, int endSektor) {
		int array_size = 5;
		int iterator = 0;
		if (startSektor >= 10)
			array_size++;
		if (endSektor >= 10)
			array_size++;
		byte[] message = new byte[array_size];
		message[iterator++] = FLASH_CONFIGURE_REQUEST;
		message[iterator++] = 0x20; // = Leerzeichen
		if (startSektor >= 10) {
			message[iterator++] = (byte) ((startSektor / 10) + 0x30);
			message[iterator++] = (byte) ((startSektor % 10) + 0x30);
		} else
			message[iterator++] = (byte) (startSektor + 0x30);
		message[iterator++] = 0x20; // = Leerzeichen
		if (endSektor >= 10) {
			message[iterator++] = (byte) ((endSektor / 10) + 0x30);
			message[iterator++] = (byte) ((endSektor % 10) + 0x30);
		} else
			message[iterator++] = (byte) (endSektor + 0x30);
		return message;
	}

	/**
	 * Pacemate Style
	 */
	public static byte[] flashEraseRequestMessage(int startSektor, int endSektor) {
		int array_size = 5;
		int iterator = 0;

		if (startSektor >= 10)
			array_size++;
		if (endSektor >= 10)
			array_size++;

		byte[] message = new byte[array_size];

		message[iterator++] = FLASH_ERASE_REQUEST;

		message[iterator++] = 0x20; // = Leerzeichen

		if (startSektor >= 10) {
			message[iterator++] = (byte) ((startSektor / 10) + 0x30);
			message[iterator++] = (byte) ((startSektor % 10) + 0x30);
		} else
			message[iterator++] = (byte) (startSektor + 0x30);

		message[iterator++] = 0x20; // = Leerzeichen

		if (endSektor >= 10) {
			message[iterator++] = (byte) ((endSektor / 10) + 0x30);
			message[iterator++] = (byte) ((endSektor % 10) + 0x30);
		} else
			message[iterator++] = (byte) (endSektor + 0x30);
		return message;
	}

	/**
	 * Pacemate Style
	 */
	public static byte[] writeToRAMRequestMessage(long address, int numberOfBytes) {
		byte[] start = Long.toString(address).getBytes();
		byte[] number = Integer.toString(numberOfBytes).getBytes();

		//System.out.println("write to ram "+start[0]+" "+start+" "+number[0]+" "+number);

		byte[] message = new byte[3 + start.length + number.length];

		int iterator = 0;

		message[iterator++] = WRITE_TO_RAM_REQUEST;

		message[iterator++] = 0x20; // = Leerzeichen

		for (int i = 0; i < start.length; i++)
			message[iterator++] = start[i];

		message[iterator++] = 0x20; // = Leerzeichen

		for (int i = 0; i < number.length; i++)
			message[iterator++] = number[i];

		return message;
	}

	/**
	 * Pacemate Style
	 */
	public static byte[] writeCRCRequestMessage(long CRC) {
		byte[] crc = Long.toString(CRC).getBytes();

		//System.out.println("transmite CRC "+crc[0]+" "+crc);

		byte[] message = new byte[crc.length];

		for (int i = 0; i < crc.length; i++)
			message[i] = crc[i];

		return message;
	}

	/**
	 * Pacemate Style
	 */
	public static byte[] copyRAMToFlashRequestMessage(long flashAddress, long ramAddress, int numberOfBytes) {
		byte[] startFlash = Long.toString(flashAddress).getBytes();
		byte[] startRAM = Long.toString(ramAddress).getBytes();
		byte[] number = Integer.toString(numberOfBytes).getBytes();

		//System.out.println("copy ram to flash"+startFlash[0]+" "+startFlash+" "+startRAM[0]+" "+startRAM+" "+number[0]+" "+number);

		byte[] message = new byte[4 + startFlash.length + startRAM.length + number.length];

		int iterator = 0;

		message[iterator++] = COPY_RAM_TO_FLASH_REQUEST;

		message[iterator++] = 0x20; // = Leerzeichen

		for (int i = 0; i < startFlash.length; i++)
			message[iterator++] = startFlash[i];

		message[iterator++] = 0x20; // = Leerzeichen

		for (int i = 0; i < startRAM.length; i++)
			message[iterator++] = startRAM[i];

		message[iterator++] = 0x20; // = Leerzeichen

		for (int i = 0; i < number.length; i++)
			message[iterator++] = number[i];

		return message;
	}

	/**
	 * Pacemate Style
	 */
	public static byte[] flashReadRequestMessage(long flashAddress, int numberOfBytes) {
		byte[] startFlash = Long.toString(flashAddress).getBytes();
		byte[] number = Integer.toString(numberOfBytes).getBytes();

		byte[] message = new byte[3 + startFlash.length + number.length];

		int iterator = 0;

		message[iterator++] = FLASH_READ_REQUEST;

		message[iterator++] = 0x20; // = Leerzeichen

		for (int i = 0; i < startFlash.length; i++)
			message[iterator++] = startFlash[i];

		message[iterator++] = 0x20; // = Leerzeichen

		for (int i = 0; i < number.length; i++)
			message[iterator++] = number[i];

		return message;
	}

	/**	*/
	public static byte[] flashProgramRequestMessage(int address, byte[] data) {

		byte[] message = new byte[1 + 4 + data.length];
		message[0] = FLASH_PROGRAM_REQUEST;
		System.arraycopy(addressToBytes(address), 0, message, 1, 4);
		System.arraycopy(data, 0, message, 5, data.length);
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

	/**
	 public static byte[] flashTypeReadRequestMessage() {
	 return new byte[] { FLASH_TYPE_READ_REQUEST };
	 }*/

	/**	 */
	public static byte[] ramReadRequestMessage(int address, int length) {
		byte[] message = new byte[1 + 4 + 2];
		message[0] = RAM_READ_REQUEST;
		System.arraycopy(addressToBytes(address), 0, message, 1, 4);
		System.arraycopy(addressToBytes(length), 0, message, 5, 2);
		return message;
	}

	/**	 */
	public static byte[] statusRegisterWriteMessage(byte status) {
		byte[] message = new byte[2];
		message[0] = WRITE_SR_REQUEST;
		message[1] = status;
		return message;
	}

}
