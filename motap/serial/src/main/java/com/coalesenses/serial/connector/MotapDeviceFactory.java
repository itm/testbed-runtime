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

package com.coalesenses.serial.connector;

import de.uniluebeck.itm.motelist.MoteList;
import de.uniluebeck.itm.motelist.MoteListFactory;
import de.uniluebeck.itm.motelist.MoteType;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 20.12.2010
 * Time: 14:00:39
 * To change this template use File | Settings | File Templates.
 */
public class MotapDeviceFactory {
    private static final Logger log = LoggerFactory.getLogger(MotapDeviceFactory.class);

	/**
	 * Creates a new {@link com.coalesenses.serial.connector.SerialConnector} instance using serial port auto-detection.
	 *
	 * @param type	   the type of the device as string (see {@link MoteType})
	 * @param macAddress the MAC address of the node
	 *
	 * @return a RemoteUARTDevice instance or {@code null} if the serial port could not be detected
	 */
	public static SerialConnector create(String type, long macAddress) {
		MoteList moteList = MoteListFactory.create(null);
		String motePort = moteList.getMotePort(MoteType.fromString(type), macAddress);
		if (motePort == null) {
			log.error("Device of type {} and MAC address {} could not be found.", type, macAddress);
			return null;
		}
		return create(type, motePort);
	}

	/**
	 * Creates a new {@link com.coalesenses.serial.connector.SerialConnector} instance using a serial device of type {@code type} that is connected to the
	 * serial port {@code port}.
	 *
	 * @param type the type of the device as string (see {@link MoteType})
	 * @param port the serial port to which the device is connected (e.g. /dev/ttyUSB0)
	 *
	 * @return a RemoteUARTDevice instance or {@code null} if the connection could not be established
	 */
	public static SerialConnector create(String type, String port) {
		try {
			iSenseDevice device = DeviceFactory.create(type, port);
			return create(device);
		} catch (Exception e) {
			log.error("Device of type {} on port {} could not be instantiated. Reason: {}", new Object[]{type, port, e}
			);
			return null;
		}
	}

	public static SerialConnector create(iSenseDevice device) {
		return new SerialConnector(device);
	}
}
