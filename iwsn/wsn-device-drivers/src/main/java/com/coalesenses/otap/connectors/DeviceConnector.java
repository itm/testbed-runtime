package com.coalesenses.otap.connectors;

import com.coalesenses.seraerial.SerAerialPacket;

import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 19.11.2010
 * Time: 13:19:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class DeviceConnector {

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

    List<DeviceConnectorListener> listeners = new Vector<DeviceConnectorListener>();

    /**
     *
     */
    private boolean confirmPending = false;

    /**
     *
     */
    public abstract void seraerialInit();

    /**
     *
     */
    public abstract void seraerialShutdown();


    public abstract boolean sendPacket(SerAerialPacket p);

    public abstract boolean send(int type, byte[] b);

    public void addListener(DeviceConnectorListener listener){
        listeners.add(listener);
    }

    public void removeListener(DeviceConnectorListener listener){
        listeners.remove(listener);
    }

    public void notifyListeners(SerAerialPacket p){
        for (DeviceConnectorListener listener : listeners) {
            listener.handleDevicePacket(p);
        }
    }
}
