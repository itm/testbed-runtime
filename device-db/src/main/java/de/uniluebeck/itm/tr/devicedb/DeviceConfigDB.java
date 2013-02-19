package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Map;

public interface DeviceConfigDB extends Service {

	Map<NodeUrn, DeviceConfig> getByNodeUrns(Iterable<NodeUrn> nodeUrns);

	DeviceConfig getByUsbChipId(String usbChipId);

	DeviceConfig getByNodeUrn(NodeUrn nodeUrn);

	DeviceConfig getByMacAddress(long macAddress);

	Iterable<DeviceConfig> getAll();

}
