package de.uniluebeck.itm.tr.iwsn.gateway;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;

public interface DeviceAdapterFactory {

	boolean canHandle(final DeviceConfig deviceConfig);

	DeviceAdapter create(final String port, final DeviceConfig deviceConfig);

}
