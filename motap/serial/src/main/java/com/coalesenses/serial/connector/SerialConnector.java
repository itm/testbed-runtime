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

import com.coalesenses.otap.core.connector.DeviceConnector;
import com.coalesenses.otap.core.seraerial.SerAerialPacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 19.11.2010
 * Time: 13:32:47
 * To change this template use File | Settings | File Templates.
 */
public class SerialConnector extends DeviceConnector {

    private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

    private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

    private static final byte NODE_OUTPUT_TEXT = 50;

    private static final byte NODE_OUTPUT_BYTE = 51;

    private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

    private static Logger log = LoggerFactory.getLogger(SerialConnector.class);

    protected iSenseDevice USBDevice;

    private iSenseDeviceListener deviceListener = new iSenseDeviceListenerAdapter() {

        @Override
        public void receivePacket(final MessagePacket p) {

            log.trace("Received message packet: {}", p);

			SerialConnector.this.receivePacket(p);
        }
    };

    public SerialConnector(iSenseDevice USBDevice) {
        this.USBDevice = USBDevice;
        this.USBDevice.registerListener(deviceListener);
    }

    @Override
    public void handleConfirm(SerAerialPacket p) {
    }


    @Override
    public void shutdown() {
        USBDevice.shutdown();
        stop = true;
    }

    @Override
    public boolean sendPacket(SerAerialPacket p) {
        if (p instanceof com.coalesenses.otap.core.seraerial.SerialRoutingPacket) {
            return send(PacketTypes.ISENSE_ISHELL_INTERPRETER & 0xFF, p.toByteArray());

        } else {
            return send(PacketTypes.SERAERIAL & 0xFF, p.toByteArray());

        }
    }

    @Override
    public boolean send(int type, byte[] b) {
        if (b == null || type > 0xFF) {
            log.warn("Skipping empty packet or type > 0xFF.");
            return false;
        }

        try {
            MessagePacket p = new MessagePacket(type & 0xFF, b);
            USBDevice.send(p);
            return true;
        } catch (Exception e) {
            log.warn("Unable to send packet:" + e, e);
            return false;
        }
    }


    public void setUSBDevice(iSenseDevice USBDevice) {
        this.USBDevice = USBDevice;
    }
}
