/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
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

package de.uniluebeck.itm.wsn.devicedrivers;

import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicBinFile;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicDevice;
import de.uniluebeck.itm.wsn.devicedrivers.mockdevice.MockDevice;
import de.uniluebeck.itm.wsn.devicedrivers.nulldevice.NullDevice;
import de.uniluebeck.itm.wsn.devicedrivers.pacemate.PacemateDevice;
import de.uniluebeck.itm.wsn.devicedrivers.telosb.TelosbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DeviceFactory {

	private static final Logger log = LoggerFactory.getLogger(DeviceFactory.class);

	public static iSenseDevice create(String type, String port) throws Exception {

		log.debug("Using java.library.path={}", System.getProperty("java.library.path"));

		if ("isense".equals(type)) {
			return new JennicDevice(port);
		} else if ("pacemate".equals(type)) {
			return new PacemateDevice(port);
		} else if ("telosb".equals(type)) {
			return new TelosbDevice(port);
		} else if ("null".equals(type)) {
			return new NullDevice();
		} else if ("mock".equals(type)) {
			return new MockDevice(port);
		}

		throw new Exception("Unknown type " + type);
	}

	public static String toHexString(byte[] tmp) {
		return toHexString(tmp, 0, tmp.length);
	}

	public static String toHexString(byte[] tmp, int offset, int length) {
		StringBuffer s = new StringBuffer();
		for (int i = offset; i < offset + length; ++i) {
			if (s.length() > 0)
				s.append(' ');
			s.append("0x");
			s.append(Integer.toHexString(tmp[i] & 0xFF));
		}
		return s.toString();
	}

	public static String toASCIIString(byte[] tmp) {
		StringBuffer sb = new StringBuffer("");

		for (byte b : tmp) {
			if (b == 0x0D)
				sb.append("<CR>");
			else if (b == 0x0A)
				sb.append("<LF>");
			else {
				char chr = (char) b;
				sb.append(chr);
			}
		}

		return sb.toString();
	}

}
