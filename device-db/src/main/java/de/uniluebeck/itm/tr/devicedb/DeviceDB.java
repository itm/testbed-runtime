package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;
import java.util.Map;

public interface DeviceDB extends Service {

	Map<NodeUrn, DeviceConfig> getConfigsByNodeUrns(Iterable<NodeUrn> nodeUrns);

	@Nullable
	DeviceConfig getConfigByUsbChipId(String usbChipId);

	@Nullable
	DeviceConfig getConfigByNodeUrn(NodeUrn nodeUrn);

	@Nullable
	DeviceConfig getConfigByMacAddress(long macAddress);

	Iterable<DeviceConfig> getAll();

	void add(DeviceConfig deviceConfig);

	boolean removeByNodeUrn(NodeUrn nodeUrn);

	void removeAll();
}
