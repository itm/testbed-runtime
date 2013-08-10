package de.uniluebeck.itm.tr.plugins.mockdeviceadapter;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapterFactory;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayScheduler;

public class MockDeviceAdapterFactory implements DeviceAdapterFactory {

	private final GatewayScheduler gatewayScheduler;

	public MockDeviceAdapterFactory(final GatewayScheduler gatewayScheduler) {
		this.gatewayScheduler = gatewayScheduler;
	}

	@Override
	public boolean canHandle(final DeviceConfig deviceConfig) {
		return true;
	}

	@Override
	public DeviceAdapter create(final String port, final DeviceConfig deviceConfig) {
		return new MockDeviceAdapter(deviceConfig, gatewayScheduler);
	}
}
