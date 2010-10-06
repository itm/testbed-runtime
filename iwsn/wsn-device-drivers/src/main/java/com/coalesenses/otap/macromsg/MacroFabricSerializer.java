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

package com.coalesenses.otap.macromsg;

import de.uniluebeck.itm.wsn.devicedrivers.generic.PacketTypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class MacroFabricSerializer {

	/**
	 * Enumeration for top-level data type selection.
	 */
	public enum DataTypeCode {

		PRESENCEDETECTREQUEST,
		PRESENCEDETECTREPLY,
		OTAPINITREQUEST,
		OTAPINITREPLY,
		OTAPPROGRAMREQUEST,
		OTAPPROGRAMREPLY,
		UNKNOWN
	}

	private static ByteArrayOutputStream os;

	private static ByteArrayInputStream is;

	/**
	 * Specifies which serializer has been used for generating this code.
	 */
	public static final String SERIALIZER_TYPE = "macrofibre";


	public static byte[] serialize(PresenceDetectRequest dtype) {
		os = new ByteArrayOutputStream();
		writeInteger(PacketTypes.OTAP, 1); // packet type
		writeInteger(0, 1);	// data type 
		return os.toByteArray();
	}

	public static PresenceDetectRequest deserialize_PresenceDetectRequest(byte[] buffer, ByteArrayOutputStream reader) {
		is = new ByteArrayInputStream(buffer);
		readInteger(1);	// packet type
		readInteger(1);	// data type
		PresenceDetectRequest dtype = new PresenceDetectRequest();
		//dtype.value = (readInteger(1) != 0L);
		return dtype;
	}


	public static byte[] serialize(PresenceDetectReply dtype) {
		os = new ByteArrayOutputStream();
		writeInteger(PacketTypes.OTAP, 1); // packet type
		writeInteger(1, 1);	// data type 
		writeInteger(dtype.device_id, 2);
		writeInteger(dtype.chip_type, 1);
		writeInteger(dtype.application_id, 2);
		writeInteger(dtype.revision_no, 1);
		writeInteger(dtype.protocol_version, 1);
		return os.toByteArray();
	}

	public static PresenceDetectReply deserialize_PresenceDetectReply(byte[] buffer, ByteArrayOutputStream reader) {
		if (buffer.length == 2 + PresenceDetectReply.header_length) {
			is = new ByteArrayInputStream(buffer);
			readInteger(1);	// packet type
			readInteger(1);	// data type
			PresenceDetectReply dtype = new PresenceDetectReply();
			dtype.device_id = (int) readInteger(2);
			dtype.chip_type = (short) readInteger(1);
			dtype.application_id = (int) readInteger(2);
			dtype.revision_no = (short) readInteger(1);
			dtype.protocol_version = (short) readInteger(1);
			return dtype;
		}
		return null;
	}


	public static byte[] serialize(OtapInitRequest dtype) {
		os = new ByteArrayOutputStream();
		writeInteger(PacketTypes.OTAP, 1); // packet type
		writeInteger(2, 1);	// data type 
		writeInteger(dtype.chunk_count, 1);
		writeInteger(dtype.timeout_multiplier_ms, 1);
		writeInteger(dtype.max_re_requests, 1);
		writeInteger(dtype.participating_devices.count, 1);
		for (int i1 = 0; i1 < dtype.participating_devices.count; ++i1) {
			writeInteger(dtype.participating_devices.value[i1], 2);
		}
		return os.toByteArray();
	}

	public static OtapInitRequest deserialize_OtapInitRequest(byte[] buffer, ByteArrayOutputStream reader) {
		is = new ByteArrayInputStream(buffer);
		readInteger(1);	// packet type
		readInteger(1);	// data type
		OtapInitRequest dtype = new OtapInitRequest();
		dtype.chunk_count = (short) readInteger(1);
		dtype.timeout_multiplier_ms = (short) readInteger(1);
		dtype.max_re_requests = (short) readInteger(1);
		dtype.participating_devices.count = (int) readInteger(1);
		for (int i1 = 0; i1 < dtype.participating_devices.count; ++i1) {
			dtype.participating_devices.value[i1] = (int) readInteger(2);
		}
		return dtype;
	}

	public static byte[] serialize(OtapInitReply dtype) {
		os = new ByteArrayOutputStream();
		writeInteger(PacketTypes.OTAP, 1); // packet type
		writeInteger(3, 1);	// data type 
		writeInteger(dtype.device_id, 2);
		return os.toByteArray();
	}

	public static OtapInitReply deserialize_OtapInitReply(byte[] buffer, ByteArrayOutputStream reader) {
		if (buffer.length == 2 + OtapInitReply.header_length) {
			is = new ByteArrayInputStream(buffer);
			readInteger(1);	// packet type
			readInteger(1);	// data type
			OtapInitReply dtype = new OtapInitReply();
			dtype.device_id = (int) readInteger(2);
			return dtype;
		}
		return null;
	}

	public static byte[] serialize(OtapProgramRequest dtype) {
		os = new ByteArrayOutputStream();
		writeInteger(PacketTypes.OTAP, 1); // packet type
		writeInteger(4, 1);	// data type 
		writeInteger(dtype.chunk_no, 1);
		writeInteger(dtype.packets_in_chunk, 1);
		writeInteger(dtype.index, 1);
		writeInteger(dtype.remaining, 1);
		writeInteger(dtype.overall_packet_no, 2);
		dtype.code.count = 64;	// fixed array element count
		for (int i1 = 0; i1 < dtype.code.count; ++i1) {
			writeInteger(dtype.code.value[i1], 1);
		}
		return os.toByteArray();
	}

	public static OtapProgramRequest deserialize_OtapProgramRequest(byte[] buffer, ByteArrayOutputStream reader) {
		is = new ByteArrayInputStream(buffer);
		readInteger(1);	// packet type
		readInteger(1);	// data type
		OtapProgramRequest dtype = new OtapProgramRequest();
		dtype.chunk_no = (short) readInteger(1);
		dtype.packets_in_chunk = (short) readInteger(1);
		dtype.index = (short) readInteger(1);
		dtype.remaining = (short) readInteger(1);
		dtype.overall_packet_no = (int) readInteger(2);
		dtype.code.count = 64;	// fixed array element count
		for (int i1 = 0; i1 < dtype.code.count; ++i1) {
			dtype.code.value[i1] = (byte) readInteger(1);
		}
		return dtype;
	}


	public static byte[] serialize(OtapProgramReply dtype) {
		os = new ByteArrayOutputStream();
		writeInteger(PacketTypes.OTAP, 1); // packet type
		writeInteger(5, 1);	// data type 
		writeInteger(dtype.device_id, 2);
		writeInteger(dtype.chunk_no, 1);
		writeInteger(dtype.missing_indices.count, 1);
		for (int i1 = 0; i1 < dtype.missing_indices.count; ++i1) {
			writeInteger(dtype.missing_indices.value[i1], 1);
		}
		return os.toByteArray();
	}

	public static OtapProgramReply deserialize_OtapProgramReply(byte[] buffer, ByteArrayOutputStream reader) {
		if (buffer.length >= 2 + OtapProgramReply.header_length) {
			is = new ByteArrayInputStream(buffer);
			readInteger(1);	// packet type
			readInteger(1);	// data type
			OtapProgramReply dtype = new OtapProgramReply();
			dtype.device_id = (int) readInteger(2);
			dtype.chunk_no = (short) readInteger(1);
			dtype.missing_indices.count = (short) readInteger(1);
			if (buffer.length == (2 + OtapProgramReply.header_length + dtype.missing_indices.count)) {
				for (int i1 = 0; i1 < dtype.missing_indices.count; ++i1) {
					dtype.missing_indices.value[i1] = (short) readInteger(1);
				}
				return dtype;
			}
			return null;
		}
		return null;
	}


	private static void writeInteger(long value, int bytes) {
		for (int i1 = 0; i1 < bytes; ++i1) {
			os.write((byte) (value >> (8 * i1) & 0xFF));
		}

	}

	private static long readInteger(int bytes) {
		long result = 0;
		for (int i1 = 0; i1 < bytes; ++i1) {
			result |= ((is.read() & 0xFF) << (8 * i1));
		}
		return result;
	}


	public static DataTypeCode getDataTypeCode(byte[] buffer) {
		is = new ByteArrayInputStream(buffer);
		readInteger(1); // skip packet type
		int read = (int) readInteger(1);
		DataTypeCode[] vals = DataTypeCode.values();
		DataTypeCode code = DataTypeCode.UNKNOWN;
		if (read >= 0 && read < vals.length) {
			code = DataTypeCode.values()[read];
		}
		return code;
	}

	public static PresenceDetectRequest deserialize_PresenceDetectRequest(byte[] buffer) {
		return deserialize_PresenceDetectRequest(buffer, null);
	}


	public static PresenceDetectReply deserialize_PresenceDetectReply(byte[] buffer) {
		return deserialize_PresenceDetectReply(buffer, null);
	}

	public static OtapInitRequest deserialize_OtapInitRequest(byte[] buffer) {
		return deserialize_OtapInitRequest(buffer, null);
	}


	public static OtapInitReply deserialize_OtapInitReply(byte[] buffer) {
		return deserialize_OtapInitReply(buffer, null);
	}

	public static OtapProgramRequest deserialize_OtapProgramRequest(byte[] buffer) {
		return deserialize_OtapProgramRequest(buffer, null);
	}


	public static OtapProgramReply deserialize_OtapProgramReply(byte[] buffer) {
		return deserialize_OtapProgramReply(buffer, null);
	}


	/**
	 * Deserializes the next object from the stream buffer.
	 *
	 * @param buffer The byte array containing the serialized data.
	 *
	 * @return The deserialized data type as an {@link Object}.
	 */
	public static Object deserialize(byte[] buffer) {
		Object o;
		switch (getDataTypeCode(buffer)) {
			case PRESENCEDETECTREQUEST:
				o = deserialize_PresenceDetectRequest(buffer);
				break;
			case PRESENCEDETECTREPLY:
				o = deserialize_PresenceDetectReply(buffer);
				break;
			case OTAPINITREQUEST:
				o = deserialize_OtapInitRequest(buffer);
				break;
			case OTAPINITREPLY:
				o = deserialize_OtapInitReply(buffer);
				break;
			case OTAPPROGRAMREQUEST:
				o = deserialize_OtapProgramRequest(buffer);
				break;
			case OTAPPROGRAMREPLY:
				o = deserialize_OtapProgramReply(buffer);
				break;
			default:
				o = null;
				break;
		}
		return o;
	}


}
