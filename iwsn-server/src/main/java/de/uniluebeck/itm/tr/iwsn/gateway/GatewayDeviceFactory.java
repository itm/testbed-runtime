package de.uniluebeck.itm.tr.iwsn.gateway;

import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.wsn.drivers.core.Device;

public interface GatewayDeviceFactory {

	GatewayDevice create(final DeviceConfig deviceConfig, final Device device);

}
