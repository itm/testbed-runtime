package de.uniluebeck.itm.tr.iwsn.gateway;

import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;

public interface GatewayDeviceAdapterFactory {

	GatewayDeviceAdapter create(final String port, final DeviceConfig deviceConfig);

}
