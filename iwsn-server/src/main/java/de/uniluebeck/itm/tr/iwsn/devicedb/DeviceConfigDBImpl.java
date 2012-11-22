package de.uniluebeck.itm.tr.iwsn.devicedb;

import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import eu.wisebed.api.v3.common.NodeUrn;

public class DeviceConfigDBImpl implements DeviceConfigDB {

	@Override
	public DeviceConfig getByUsbChipId(final String usbChipId) {
		return null;  // TODO implement
	}

	@Override
	public DeviceConfig getByNodeUrn(final NodeUrn nodeUrn) {
		return null;  // TODO implement
	}

	@Override
	public DeviceConfig getByMacAddress(final MacAddress macAddress) {
		return null;  // TODO implement
	}
}
