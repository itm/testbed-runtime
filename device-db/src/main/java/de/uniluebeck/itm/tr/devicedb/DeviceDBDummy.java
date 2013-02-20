package de.uniluebeck.itm.tr.devicedb;

import com.google.common.util.concurrent.AbstractService;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class DeviceDBDummy extends AbstractService implements DeviceDB {

	private static final NodeUrn DEVELOPMENT_DEVICE_NODE_URN = new NodeUrn("urn:wisebed:uzl1:0x2087");

	private static final DeviceConfig DEVELOPMENT_DEVICE_CONFIG = new DeviceConfig(
			DEVELOPMENT_DEVICE_NODE_URN,
			"isense48",
			false,
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
	protected void doStart() {
		try {
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public Map<NodeUrn, DeviceConfig> getConfigsByNodeUrns(final Iterable<NodeUrn> nodeUrns) {
		final HashMap<NodeUrn,DeviceConfig> map = newHashMap();
		map.put(DEVELOPMENT_DEVICE_NODE_URN, DEVELOPMENT_DEVICE_CONFIG);
		return map;
	}

	@Override
	public DeviceConfig getConfigByUsbChipId(final String usbChipId) {
		return DEVELOPMENT_DEVICE_CONFIG;
	}

	@Override
	public DeviceConfig getConfigByNodeUrn(final NodeUrn nodeUrn) {
		return DEVELOPMENT_DEVICE_CONFIG;
	}

	@Override
	public DeviceConfig getConfigByMacAddress(final long macAddress) {
		return DEVELOPMENT_DEVICE_CONFIG;
	}

	@Override
	public Iterable<DeviceConfig> getAll() {
		return newArrayList(DEVELOPMENT_DEVICE_CONFIG);
	}

	@Override
	public void add(final DeviceConfig deviceConfig) {
		// TODO implement
	}

	@Override
	public boolean removeByNodeUrn(final NodeUrn nodeUrn) {
		return false;  // TODO implement
	}

	@Override
	public void removeAll() {
		// TODO implement
	}
}
