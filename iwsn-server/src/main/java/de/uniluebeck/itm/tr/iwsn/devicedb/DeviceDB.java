package de.uniluebeck.itm.tr.iwsn.devicedb;

import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import eu.wisebed.api.v3.common.NodeUrn;

public interface DeviceDB {

	DeviceData getByUsbChipId(String usbChipId);

	DeviceData getByNodeUrn(NodeUrn nodeUrn);

	DeviceData getByMacAddress(MacAddress macAddress);
}
