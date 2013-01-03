package de.uniluebeck.itm.tr.iwsn.devicedb;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.newHashMap;

public class DeviceConfigDBImpl implements DeviceConfigDB {

	private static final NodeUrn DEVELOPMENT_DEVICE_NODE_URN = new NodeUrn("urn:wisebed:uzl1:0x2087");

	private static final DeviceConfig DEVELOPMENT_DEVICE_CONFIG = new DeviceConfig(
			DEVELOPMENT_DEVICE_NODE_URN,
			"isense48",
			null,
			null,
			null,
			null,
			null,
			TimeUnit.SECONDS.toMillis(1),
			TimeUnit.MINUTES.toMillis(2),
			TimeUnit.SECONDS.toMillis(5),
			TimeUnit.SECONDS.toMillis(2)
	);

	@Override
	public Map<NodeUrn, DeviceConfig> getByNodeUrns(final Iterable<NodeUrn> nodeUrns) {
		final HashMap<NodeUrn,DeviceConfig> map = newHashMap();
		map.put(DEVELOPMENT_DEVICE_NODE_URN, DEVELOPMENT_DEVICE_CONFIG);
		return map;
	}

	@Override
	public DeviceConfig getByUsbChipId(final String usbChipId) {
		return DEVELOPMENT_DEVICE_CONFIG;
	}

	@Override
	public DeviceConfig getByNodeUrn(final NodeUrn nodeUrn) {
		return DEVELOPMENT_DEVICE_CONFIG;
	}

	@Override
	public DeviceConfig getByMacAddress(final long macAddress) {
		return DEVELOPMENT_DEVICE_CONFIG;
	}
}
