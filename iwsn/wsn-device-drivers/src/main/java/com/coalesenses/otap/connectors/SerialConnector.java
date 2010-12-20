package com.coalesenses.otap.connectors;

import com.coalesenses.otap.OtapFlasher;
import com.coalesenses.otap.OtapInit;
import com.coalesenses.otap.PresenceDetect;
import com.coalesenses.otap.macromsg.MacroFabricSerializer;
import com.coalesenses.seraerial.SerAerialPacket;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.PacketTypes;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 19.11.2010
 * Time: 13:32:47
 * To change this template use File | Settings | File Templates.
 */
public class SerialConnector extends DeviceConnector{

    private static Logger log = LoggerFactory.getLogger(SerialConnector.class );

    protected iSenseDevice USBDevice;

    @Override
	public void seraerialInit() {


		//gui = new OtapPluginGui(this);

	}

    @Override
    public void seraerialShutdown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean sendPacket(SerAerialPacket p) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
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

	public void seraerialHandlePacket(SerAerialPacket p) {
		
	}

    /**
     *
     */
    public final boolean seraerialTransmit(SerAerialPacket p) {
        boolean ok = dispatcher.enqueue(this, p);
        TimeDiff timeout = new TimeDiff(DispatcherThread.MAX_LOCK_WAIT_MS);

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
     * Sends a packet over the serial port of the plugin's device monitor.
     *
     * @param type The packet type.
     * @param b    The actual packet as a byte array.
     */
    public synchronized final void sendPacket(int type, byte[] b) {


    }

    public void setUSBDevice(iSenseDevice USBDevice) {
        this.USBDevice = USBDevice;
    }
}
