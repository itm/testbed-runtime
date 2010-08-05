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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.motelist;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MacAddress;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.Operation;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDeviceListenerAdapter;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: bimschas
 * Date: 27.07.2010
 * Time: 20:51:12
 * TODO change
 */
public abstract class AbstractMoteList {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 *
	 * @return mapping type -> {port}
	 */
	abstract Multimap<String, String> getMoteList();

	/**
	 * port -> (type, mac)
	 */
	private BiMap<String, Tuple<String, MacAddress>> devices;

	/**
	 * @param type	   one of "isense", "telosb", "pacemate"
	 * @param macAddress
	 * @return {@code null} if not found, serial device (e.g. /dev/ttyUSB0) otherwise
	 */
	public String getMotePort(String type, long macAddress) {

		Multimap<String, String> moteList = getMoteList();

		/* port -> (type,mac) */
		if (devices == null) {

			devices = HashBiMap.create();

			for (final String port : moteList.get(type)) {

				log.debug("Probing {}", port);

				TimeDiff diff = new TimeDiff(1000);
				JennicDevice device = new JennicDevice(port);

				if (device.isConnected()) {
					device.registerListener(new iSenseDeviceListenerAdapter() {

						@Override
						public void receivePacket(final MessagePacket p) {/* nothing to do */}

						@Override
						public void operationDone(final Operation op, final Object result) {
							if (result instanceof Exception) {
								Exception e = (Exception) result;
								log.error("Caught exception when trying to read MAC address: " + e, e);
							} else if (op == Operation.READ_MAC && result != null) {
								log.debug("Found iSense device on port {} with MAC address {}", port, result);
								devices.put(port, new Tuple<String, MacAddress>("isense", (MacAddress) result));
							}
						}

					}
					);
					device.triggerGetMacAddress(true);
				}

				while (!diff.isTimeout()) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						log.error("" + e, e);
					}
				}

				if (device.getOperation() == Operation.READ_MAC) {
					device.cancelOperation(Operation.READ_MAC);
				}

				// TODO pacemate and telosb

				device.shutdown();

			}

		}

		for (Map.Entry<String, Tuple<String, MacAddress>> entry : devices.entrySet()) {
			boolean sameType = entry.getValue().getFirst().equals(type);
			boolean sameMac = entry.getValue().getSecond().getMacLowest16() == macAddress;
			if (sameType && sameMac) {
				return entry.getKey(); // port
			}
		}

		return null;

	}

}
