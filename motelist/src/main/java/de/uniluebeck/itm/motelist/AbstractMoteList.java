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
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MacAddress;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.Operation;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDeviceListenerAdapter;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Map;


abstract class AbstractMoteList implements MoteList {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private ProcessBuilder pb;

	private File tmpFile;

	@Override
	public String getMotePort(MoteType type, long macAddress) {

		// create a mapping from node ports to the MAC addresses of the devices that are attached to them
		BiMap<String, MacAddress> map;
		Collection<String> portList = getMoteList().get(type);
		if (MoteType.ISENSE == type) {
			map = createISenseMap(portList);
		} else if (MoteType.TELOSB == type) {
			throw new RuntimeException("TelosB nodes are not supported since it is not possible to read out the MAC "
					+ "address of a node by using hardware functions."
			);
		} else if (MoteType.PACEMATE == type) {
			map = createPacemateMap(portList);
		} else {
			throw new RuntimeException("This only happens if you forgot to add a new MoteType to this check ;-)");
		}

		// check for equality and return node if found
		byte[] searchLower16 = new byte[2];
		searchLower16[0] = (byte) (macAddress >> 8 & 0xFF);
		searchLower16[1] = (byte) (macAddress & 0xFF);

		log.debug("Searching for device with MAC address: {}", StringUtils.toHexString(searchLower16));

		for (Map.Entry<String, MacAddress> entry : map.entrySet()) {

			byte[] found = entry.getValue().getMacBytes();
			byte[] foundLower16 = new byte[2];
			foundLower16[0] = found[6];
			foundLower16[1] = found[7];

			log.debug("{} == {} ?", StringUtils.toHexString(searchLower16), StringUtils.toHexString(foundLower16));

			boolean sameMac =
					searchLower16[0] == foundLower16[0] &&
							searchLower16[1] == foundLower16[1];

			if (sameMac) {
				return entry.getKey(); // port
			}
		}

		// no node with the given MAC address was found so return null
		return null;

	}

	@Override
	public Multimap<MoteType, String> getMoteList() {

		File tmpFile = copyScriptToTmpFile();
		pb = new ProcessBuilder(tmpFile.getAbsolutePath(), "-c");

		BufferedReader in;
		try {
			Process p = pb.start();
			// Eingabestream holen
			in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			// parsing
			return parseMoteList(in);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private File copyScriptToTmpFile() {

		if (tmpFile == null || !tmpFile.exists()) {

			try {

				InputStream from = getClass().getClassLoader().getResourceAsStream(getScriptName());
				FileOutputStream to = null;

				try {

					tmpFile = File.createTempFile("motelist", "");
					to = new FileOutputStream(tmpFile);
					byte[] buffer = new byte[4096];
					int bytesRead;

					while ((bytesRead = from.read(buffer)) != -1) {
						to.write(buffer, 0, bytesRead);
					} // write

				} finally {
					if (from != null) {
						try {
							from.close();
						} catch (IOException e) {
							log.debug("" + e, e);
						}
					}
					if (to != null) {
						try {
							to.close();
						} catch (IOException e) {
							log.debug("" + e, e);
						}
					}
				}

				tmpFile.setExecutable(true);
				return tmpFile;

			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

		return tmpFile;

	}

	protected abstract Multimap<MoteType, String> parseMoteList(final BufferedReader in);

	/**
	 * @param ports
	 *
	 * @return a mapping port -> (type, macaddress)
	 */
	private BiMap<String, MacAddress> createISenseMap(Collection<String> ports) {

		final BiMap<String, MacAddress> devices = HashBiMap.create();

		for (final String port : ports) {

			log.debug("Probing {}", port);

			TimeDiff diff = new TimeDiff(1000);
			JennicDevice device = new JennicDevice(port);

			if (device.isConnected()) {
				log.info("Connected to device at {}", port);
				device.registerListener(new iSenseDeviceListenerAdapter() {

					@Override
					public void receivePacket(final MessagePacket p) {/* nothing to do */}

					@Override
					public void operationDone(final Operation op, final Object result) {
						if (result instanceof Exception) {
							Exception e = (Exception) result;
							log.error("Caught exception when trying to read MAC address: " + e, e);
						} else if (op == Operation.READ_MAC && result != null) {
							log.info("Found iSense device on port {} with MAC address {}", port, result);
							devices.put(port, (MacAddress) result);
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

			device.shutdown();

		}

		return devices;
	}

	private BiMap<String, MacAddress> createPacemateMap(final Collection<String> portList) {
		return null;  // TODO implement
	}

	private BiMap<String, MacAddress> createTelosBMap(final Collection<String> portList) {
		return null;  // TODO implement
	}

	public abstract String getScriptName();
}
