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

package com.coalesenses.seraerial;

import com.coalesenses.otap.connectors.DeviceConnector;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.PacketTypes;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

//	-------------------------------------------------------------------------

/**
 * @author Dennis Pfisterer
 */
public class SerAerialPlugin {
    private DeviceConnector connector = null;

    static class DispatcherThread extends Thread {

        /**
         *
         */
        private static final Logger log = LoggerFactory.getLogger(DispatcherThread.class);

        

        /**
         *
         */
        private final static int MAX_PENDING_PACKETS = 50;

        /**
         *
         */
        private LinkedList<SerAerialPacket> pendingPackets = new LinkedList<SerAerialPacket>();

        /**
         *
         */
        private TimeDiff confirmPendingSince = new TimeDiff(DeviceConnector.MAX_LOCK_WAIT_MS);

        /**
         *
         */
        private boolean confirmPending = false;

        /**
         *
         */
        private SerAerialPacket lastPacket = null;



        /**
         *
         */
        private long lastConfirmId = -1;

        /**
         *
         */
        synchronized boolean enqueue(SerAerialPlugin plugin, SerAerialPacket packet) {
            if (pendingPackets.size() > MAX_PENDING_PACKETS) {
                log.warn("Too many pending packets. Dropped enqueue request.");
                return false;
            }
            packet.setSender(SerAerialPlugin.this.connector);
            plugin.confirmPending = true;
            pendingPackets.addLast(packet);
            //log.debug("Enqueued packet(now pending: " + pendingPackets.size() + ") from " + plugin + ": " + packet);
            notify();
            return true;
        }

        /**
         * @param packetId
         */
        public void notifyConfirmReceived(SerAerialPacket confirmPacket, long packetId) {
            //One confirm is received multiple times (all plugins see this) -> handle only once

            synchronized (this) {
                if (packetId == lastConfirmId) {
                    return;
                }
                lastConfirmId = packetId;
            }

            if (lastPacket != null && confirmPacket != null && lastPacket.getSender() != null) {
                lastPacket.getSender().setConfirmPending(false);
                synchronized (lastPacket.getSender()) {
                    lastPacket.getSender().notifyAll();
                }
                lastPacket.getSender().seraerialHandleConfirm(confirmPacket);
            }

            //log.debug("Received confirm [now pending: " + pendingPackets.size() + "]");

            synchronized (this) {
                confirmPending = false;
                notifyAll();
            }

        }

        /**
         *
         */
        public void run() {
            while (!stop) {
                synchronized (this) {
                    //Wait until packets are in the queue
                    while (pendingPackets.size() == 0 && !stop) {
                        try {
                            wait(200);
                        } catch (InterruptedException e) {
                            log.debug("Interrupted: " + e, e);
                        }
                    }

                    //Check if we may send (timeout or no pending confirm)
                    if (!stop && confirmPending == false || confirmPendingSince.isTimeout()) {

                        if (confirmPending == false) {
                            log.debug(
                                    "SerAerial transmit, no confirm pending [now pending: " + pendingPackets.size()
                                            + "]");
                        } else {
                            log.debug(
                                    "SerAerial transmit, timeout after " + confirmPendingSince.ms()
                                            + "ms) [now pending: " + pendingPackets.size() + "]");
                        }

                        if (confirmPending && lastPacket != null && lastPacket.getSender() != null) {
                            lastPacket.getSender().setConfirmPending(false);
                            lastPacket.getSender().seraerialHandleConfirm(null);
                        }

                        lastPacket = pendingPackets.removeFirst();
                        confirmPending = true;
                        confirmPendingSince.touch();
                        if (lastPacket instanceof SerialRoutingPacket) {
                            lastPacket.getSender()
                                    .sendPacket(PacketTypes.ISENSE_ISHELL_INTERPRETER, lastPacket.toByteArray());
                        } else {
                            lastPacket.getSender().sendPacket(PacketTypes.SERAERIAL, lastPacket.toByteArray());
                        }
                    } else {
                        try {
                            Thread.sleep(50);
                        } catch (Throwable e) {
                        }
                    }

                }
            }

        }

    }

    /**
     *
     */
    private static final Logger log = LoggerFactory.getLogger(SerAerialPlugin.class);

    /**
     *
     */
    protected static DispatcherThread dispatcher = new DispatcherThread();

    /**
     *
     */
    boolean confirmPending = false;

    protected iSenseDevice USBDevice;

    /**
     *
     */
    static {
        dispatcher.start();
    }


    private static boolean stop;

    public boolean isStop() {
        return stop;
    }

    public void stopSerial() {
        this.stop = true;
    }

    /**
     *
     */
    public final int[] init() {
        connector.seraerialInit();
        return new int[]{PacketTypes.SERAERIAL};
    }

    /**
     *
     */
    public final void receivePacket(MessagePacket p) {
        SerAerialPacket seraerialPacket = new SerAerialPacket();
        seraerialPacket.parse(p);

        if (seraerialPacket.getPacketType() == SerAerialPacket.PacketType.Packet) {
            //log.debug("Received seraerial packet of type " + Tools.toHexString(seraerialPacket.getContent()[0]));
            try {
                connector.seraerialHandlePacket(seraerialPacket);
            } catch (Throwable t) {
                log.error("Error in plugin SerAerialPlugin: " + t, t);
            }
        } else if (seraerialPacket.getPacketType() == SerAerialPacket.PacketType.Confirm) {
            try {
                dispatcher.notifyConfirmReceived(seraerialPacket, p.getId());
            } catch (Throwable t) {
                log.error("Error in plugin SerAerialPlugin: " + t, t);
            }
        } else {
            log.error("[PacketId: " + p.getId() + "] UNKNOWN seraerial response received: " + p);
        }
    }

    /**
     *
     */
    public final void shutdown() {
        connector.seraerialShutdown();
    }

    /**
     *
     */
    public final boolean seraerialTransmit(SerAerialPacket p) {
        boolean ok = dispatcher.enqueue(this, p);
        TimeDiff timeout = new TimeDiff(DeviceConnector.MAX_LOCK_WAIT_MS);

        //If packet was enqueued, wait for confirm
        if (ok) {

            synchronized (this) {
                while (confirmPending && timeout.noTimeout()) {
                    try {
                        wait(100);
                    } catch (Throwable t) {
                        log.warn("Error while waiting for confirm: " + t, t);
                    }
                }

                if (timeout.isTimeout()) {
                    log.warn("Confirm lost. Continuing after timeout.");
                }

                confirmPending = false;
                return true;
            }
        }

        return false;
    }

    /**
     *
     */
    public String toString() {
        return "SerAerialPlugin";
    }

    /**
     *
     */
    public final boolean seraerialBroadcast(SerAerialPacket p) {
        p.setDest(0xFFFF);
        return seraerialTransmit(p);
    }

    /**
     *
     */
    public final boolean seraerialConfirmPending() {
        return confirmPending;
    }



    /**
     * Sends a packet over the serial port of the plugin's device monitor.
     *
     * @param type The packet type.
     * @param b    The actual packet as a byte array.
     */
    public synchronized final void sendPacket(int type, byte[] b) {

        if (b == null || type > 0xFF) {
            log.warn("Skipping empty packet or type > 0xFF.");
            return;
        }

        try {
            MessagePacket p = new MessagePacket(type & 0xFF, b);
            USBDevice.send(p);
        } catch (Exception e) {
            log.warn("Unable to send packet:" + e, e);
        }
    }


    public iSenseDevice getUSBDevice() {
        return USBDevice;
    }

    public void setUSBDevice(iSenseDevice USBDevice) {
        this.USBDevice = USBDevice;
    }



}
