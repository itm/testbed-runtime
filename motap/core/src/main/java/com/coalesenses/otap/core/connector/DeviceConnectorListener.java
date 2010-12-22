package com.coalesenses.otap.core.connector;

import com.coalesenses.otap.core.seraerial.SerAerialPacket;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 19.11.2010
 * Time: 14:48:26
 * To change this template use File | Settings | File Templates.
 */
public interface DeviceConnectorListener {

    public void handleDevicePacket(SerAerialPacket p);
}
