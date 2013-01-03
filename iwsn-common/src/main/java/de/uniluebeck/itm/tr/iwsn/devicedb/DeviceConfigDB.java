package de.uniluebeck.itm.tr.iwsn.devicedb;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Map;

public interface DeviceConfigDB {

	Map<NodeUrn, DeviceConfig> getByNodeUrns(Iterable<NodeUrn> nodeUrns);

	DeviceConfig getByUsbChipId(String usbChipId);

	DeviceConfig getByNodeUrn(NodeUrn nodeUrn);

	DeviceConfig getByMacAddress(long macAddress);
}
