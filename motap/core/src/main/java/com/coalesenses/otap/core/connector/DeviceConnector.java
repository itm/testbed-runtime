package com.coalesenses.otap.core.connector;

import com.coalesenses.otap.core.seraerial.SerAerialPacket;
import com.coalesenses.otap.core.seraerial.SerialRoutingPacket;
import de.uniluebeck.itm.tr.util.AbstractListenable;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.PacketTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 19.11.2010
 * Time: 13:19:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class DeviceConnector extends AbstractListenable<DeviceConnectorListener>{

    private static Logger log = LoggerFactory.getLogger(DeviceConnector.class);

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
        protected boolean confirmPending = false;

        /**
         *
         */
        private com.coalesenses.otap.core.seraerial.SerAerialPacket lastPacket = null;


        /**
         *
         */
        private long lastConfirmId = -1;

        /**
         *
         */
        synchronized boolean enqueue(DeviceConnector connector, SerAerialPacket packet) {
//            log.error("enqueing packet ");
            if (pendingPackets.size() > MAX_PENDING_PACKETS) {
                log.error("Too many pending packets. Dropped enqueue request.");
                return false;
            }
            packet.setSender(connector);
            connector.confirmPending = true;
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
                lastPacket.getSender().notifyListeners(confirmPacket);
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
//                log.error("and again");
                synchronized (this) {
                    //Wait until packets are in the queue
                    while (pendingPackets.size() == 0 && !stop) {
                        try {
//                            log.error("waiting packet size = 0");
                            wait(200);
                        } catch (InterruptedException e) {
                            //Thread.currentThread().interrupt();
                            log.debug("Interrupted: " + e, e);
                        }
                    }

//                    log.error("packets available size = " + pendingPackets.size());
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
                            lastPacket.getSender().handleConfirm(null);
                        }

                        lastPacket = pendingPackets.removeFirst();
                        confirmPending = true;
                        confirmPendingSince.touch();
                        if (lastPacket instanceof SerialRoutingPacket) {
//                            log.error("sending routing packet " + lastPacket);
                            lastPacket.getSender()
                                    .send(PacketTypes.ISENSE_ISHELL_INTERPRETER, lastPacket.toByteArray());
                        } else {
//                            log.error("sending seraerial packet " + lastPacket);
                            lastPacket.getSender().send(PacketTypes.SERAERIAL, lastPacket.toByteArray());
                        }
                    } else {
                        try {
//                            log.error("sleeping(50)");
                            Thread.sleep(50);
//                            log.error("woken up");
                        } catch (Throwable e) {
                        }
                    }

                }
            }
            log.error("done stop = " + stop);

        }

    }

    protected static boolean stop = false;

    /**
     *
     */
    protected static DispatcherThread dispatcher = new DispatcherThread();

    static {
        dispatcher.start();
    }


    /**
     *
     */
    public final static int MAX_LOCK_WAIT_MS = 1200;

    public boolean isConfirmPending() {
        return confirmPending;
    }

    public void setConfirmPending(boolean confirmPending) {
        this.confirmPending = confirmPending;
    }

    /**
     *
     */
    boolean confirmPending = false;


    public abstract void handleConfirm(com.coalesenses.otap.core.seraerial.SerAerialPacket p);


    /**
     *
     */
    public abstract void shutdown();


    public abstract boolean sendPacket(com.coalesenses.otap.core.seraerial.SerAerialPacket p);

    public abstract boolean send(int type, byte[] b);

    public void notifyListeners(SerAerialPacket p) {
        for (DeviceConnectorListener listener : listeners) {
            listener.handleDevicePacket(p);
        }
    }

    /**
     *
     */
    public final boolean transmit(SerAerialPacket p) {
        boolean ok = dispatcher.enqueue(this, p);
        TimeDiff timeout = new TimeDiff(MAX_LOCK_WAIT_MS);

        //If packet was enqueued, wait for confirm
        if (ok) {

            synchronized (this) {
                while (confirmPending && timeout.noTimeout()) {
                    try {
                        wait(100);
                    } catch (Throwable t) {
                        //log.warn("interruption while waiting for confirm",t);
                        Thread.currentThread().interrupt();
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

    public final SerAerialPacket receivePacket(MessagePacket p) {
        SerAerialPacket seraerialPacket = new com.coalesenses.otap.core.seraerial.SerAerialPacket();
        seraerialPacket.parse(p);

        if (seraerialPacket.getPacketType() == com.coalesenses.otap.core.seraerial.SerAerialPacket.PacketType.Packet) {
            //log.debug("Received seraerial packet of type " + Tools.toHexString(seraerialPacket.getContent()[0]));
            try {
                notifyListeners(seraerialPacket);
            } catch (Throwable t) {
                log.error("Error in plugin SerAerialPlugin: " + t, t);
            }
        } else if (seraerialPacket.getPacketType() == com.coalesenses.otap.core.seraerial.SerAerialPacket.PacketType.Confirm) {
            try {
                dispatcher.notifyConfirmReceived(seraerialPacket, p.getId());
            } catch (Throwable t) {
                log.error("Error in plugin SerAerialPlugin: " + t, t);
            }
        } else {
            log.error("[PacketId: " + p.getId() + "] UNKNOWN seraerial response received: " + p);
        }
        return seraerialPacket;
    }
}
