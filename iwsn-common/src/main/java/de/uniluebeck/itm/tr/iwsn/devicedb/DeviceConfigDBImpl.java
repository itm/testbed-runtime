package de.uniluebeck.itm.tr.iwsn.devicedb;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.concurrent.TimeUnit;

public class DeviceConfigDBImpl implements DeviceConfigDB {

	private static final DeviceConfig DEVELOPMENT_DEVICE_CONFIG = new DeviceConfig(
			"urn:wisebed:uzl1:0x2087",
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
