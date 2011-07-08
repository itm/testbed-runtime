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
import de.uniluebeck.itm.wsn.devicedrivers.pacemate.PacemateDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Map;


abstract class AbstractMoteList implements MoteList {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ProcessBuilder pb;

    private File tmpFile;

    private Map<String, String> telosBReferenceToMACMap;

    public AbstractMoteList(Map<String, String> telosBReferenceToMACMap) {
        this.telosBReferenceToMACMap = telosBReferenceToMACMap;
    }

    @Override
    public String getMotePort(MoteType type, long macAddress) {

        // create a mapping from node ports to the MAC addresses of the devices that are attached to them
        BiMap<String, MacAddress> map;
        Collection<MoteData> portList = getMoteList().get(type);
        if (MoteType.ISENSE == type) {
            map = createISenseMap(portList);
        } else if (MoteType.TELOSB == type) {
            if (telosBReferenceToMACMap == null) {
                throw new RuntimeException("TelosB nodes are not supported since it is not possible to read out the MAC "
                        + "address of a node by using hardware functions. If you whatsoever want to detect Telos B " +
                        "motes please pass in a Map instance that MAPs Telos B USB chip IDs to their corresponding MAC " +
                        "address."
                );
            } else {
                map = createTelosBMap(portList);
            }
        } else if (MoteType.PACEMATE == type) {
            map = createPacemateMap(portList);
        } else {
            throw new RuntimeException("This only happens if you forgot to add a new MoteType to this check ;-)");
        }

        // check for equality and return node if found
        byte[] searchLower16 = new byte[2];
        searchLower16[0] = (byte) (macAddress >> 8 & 0xFF);
        searchLower16[1] = (byte) (macAddress & 0xFF);

		if (log.isDebugEnabled()) {
			log.debug("Searching for {} device with MAC address: {}", type, StringUtils.toHexString(searchLower16));
		}

        for (Map.Entry<String, MacAddress> entry : map.entrySet()) {

            byte[] found = entry.getValue().getMacBytes();
            byte[] foundLower16 = new byte[2];
            foundLower16[0] = found[6];
            foundLower16[1] = found[7];

			if (log.isDebugEnabled()) {
				log.debug("{} == {} ?", StringUtils.toHexString(searchLower16), StringUtils.toHexString(foundLower16));
			}

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
    public Multimap<MoteType, MoteData> getMoteList() {

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

    protected abstract Multimap<MoteType, MoteData> parseMoteList(final BufferedReader in);

    /**
     * @param datas
     * @return a mapping port -> (type, macaddress)
     */
    private BiMap<String, MacAddress> createISenseMap(Collection<MoteData> datas) {

        final BiMap<String, MacAddress> devices = HashBiMap.create();

        for (final MoteData data : datas) {

            log.debug("Probing {}", data);

            TimeDiff diff = new TimeDiff(5000);
            JennicDevice device = new JennicDevice(data.port);

            if (device.isConnected()) {
                log.info("Connected to device at {}", data.port);
                device.registerListener(new iSenseDeviceListenerAdapter() {

                    @Override
                    public void receivePacket(final MessagePacket p) {/* nothing to do */}

                    @Override
                    public void operationDone(final Operation op, final Object result) {
                        if (result instanceof Exception) {
                            Exception e = (Exception) result;
                            log.error("Caught exception when trying to read MAC address: " + e, e);
                        } else if (op == Operation.READ_MAC && result != null) {
                            log.info("Found iSense device on port {} with MAC address {}", data.port, result);
                            devices.put(data.port, (MacAddress) result);
                        }
                    }

                }
                );
                device.triggerGetMacAddress(true);
            }
            else{
            	log.error("Device not connected");
            }

            while (!diff.isTimeout() && !devices.containsKey(data.port)) {
                try {
                	Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.error("" + e, e);
                }
            }

            if (!devices.containsKey(data.port) && device.getOperation() == Operation.READ_MAC) {
                device.cancelOperation(Operation.READ_MAC);
            }

            device.shutdown();

        }

        return devices;
    }

    private BiMap<String, MacAddress> createPacemateMap(final Collection<MoteData> portList) {

        final BiMap<String, MacAddress> devices = HashBiMap.create();

        for (final MoteData data : portList) {

            log.debug("Probing {}", data.port);

            TimeDiff diff = new TimeDiff(5000);
            PacemateDevice device = new PacemateDevice(data.port);

            if (device.isConnected()) {
                log.info("Connected to device at {}", data.port);
                device.registerListener(new iSenseDeviceListenerAdapter() {

                    @Override
                    public void receivePacket(final MessagePacket p) {/* nothing to do */}

                    @Override
                    public void operationDone(final Operation op, final Object result) {
                        if (result instanceof Exception) {
                            Exception e = (Exception) result;
                            log.error("Caught exception when trying to read MAC address: " + e, e);
                        } else if (op == Operation.READ_MAC && result != null) {
                            log.info("Found Pacemate device on port {} with MAC address {}", data.port, result);
                            devices.put(data.port, (MacAddress) result);
                        }
                    }

                }
                );
                device.triggerGetMacAddress(true);
            }

            while (!diff.isTimeout() && !devices.containsKey(data.port)) {
                try {
                	Thread.sleep(50);
                } catch (InterruptedException e) {
                    log.error("" + e, e);
                }
            }

            if (!devices.containsKey(data.port) && device.getOperation() == Operation.READ_MAC) {
                device.cancelOperation(Operation.READ_MAC);
            }

            device.shutdown();

        }

        return devices;

    }

    private BiMap<String, MacAddress> createTelosBMap(final Collection<MoteData> portList) {
        BiMap<String, MacAddress> retMap = HashBiMap.create(portList.size());
        for (MoteData data : portList) {
            if (telosBReferenceToMACMap.containsKey(data.reference)) {
                Long mac = StringUtils.parseHexOrDecLong(telosBReferenceToMACMap.get(data.reference));
                MacAddress address = new MacAddress(mac.intValue());
                retMap.put(data.port, address);
                log.info("Found Telos B device on port {} with MAC address {}", data.port, address);
            }
        }
        return retMap;
    }

    public abstract String getScriptName();
}
