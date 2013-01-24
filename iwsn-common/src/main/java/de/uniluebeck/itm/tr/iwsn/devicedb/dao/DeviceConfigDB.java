package de.uniluebeck.itm.tr.iwsn.devicedb.dao;

import de.uniluebeck.itm.tr.iwsn.devicedb.entity.DeviceConfig;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Map;

public interface DeviceConfigDB extends GenericDao<DeviceConfig, String>{

	Map<NodeUrn, DeviceConfig> getByNodeUrns(Iterable<NodeUrn> nodeUrns);

	DeviceConfig getByUsbChipId(String usbChipId);

	DeviceConfig getByNodeUrn(NodeUrn nodeUrn);

	DeviceConfig getByMacAddress(long macAddress);

}
