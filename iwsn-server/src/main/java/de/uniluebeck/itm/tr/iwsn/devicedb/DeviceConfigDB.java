package de.uniluebeck.itm.tr.iwsn.devicedb;

import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import eu.wisebed.api.v3.common.NodeUrn;

public interface DeviceConfigDB {

	DeviceConfig getByUsbChipId(String usbChipId);

	DeviceConfig getByNodeUrn(NodeUrn nodeUrn);

	DeviceConfig getByMacAddress(MacAddress macAddress);
}
