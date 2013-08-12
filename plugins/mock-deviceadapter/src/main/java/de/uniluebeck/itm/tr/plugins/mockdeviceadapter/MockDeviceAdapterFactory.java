package de.uniluebeck.itm.tr.plugins.mockdeviceadapter;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapterFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerService;

public class MockDeviceAdapterFactory implements DeviceAdapterFactory {

	private final SchedulerService schedulerService;

	public MockDeviceAdapterFactory(final SchedulerService schedulerService) {
		this.schedulerService = schedulerService;
	}

	@Override
	public boolean canHandle(final DeviceConfig deviceConfig) {
		return true;
	}

	@Override
	public DeviceAdapter create(final String port, final DeviceConfig deviceConfig) {
		return new MockDeviceAdapter(port, deviceConfig, schedulerService);
	}
}
