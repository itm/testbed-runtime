package de.uniluebeck.itm.tr.iwsn.devicedb;

import eu.wisebed.api.v3.common.NodeUrn;

public interface DeviceConfigDB {

	DeviceConfig getByUsbChipId(String usbChipId);

	DeviceConfig getByNodeUrn(NodeUrn nodeUrn);

	DeviceConfig getByMacAddress(long macAddress);
}
