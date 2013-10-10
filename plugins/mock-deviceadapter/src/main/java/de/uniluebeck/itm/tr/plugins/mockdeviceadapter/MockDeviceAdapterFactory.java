package de.uniluebeck.itm.tr.plugins.mockdeviceadapter;

import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapter;
import de.uniluebeck.itm.tr.iwsn.gateway.DeviceAdapterFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerService;

import javax.annotation.Nullable;
import java.util.Map;

public class MockDeviceAdapterFactory implements DeviceAdapterFactory {

	private final SchedulerService schedulerService;

	public MockDeviceAdapterFactory(final SchedulerService schedulerService) {
		this.schedulerService = schedulerService;
	}

	@Override
	public boolean canHandle(final String deviceType,
							 final String devicePort,
							 @Nullable final Map<String, String> deviceConfiguration,
							 @Nullable final DeviceConfig deviceConfig) {
		return true;
	}

	@Override
	public DeviceAdapter create(final String deviceType,
								final String devicePort,
								@Nullable final Map<String, String> deviceConfiguration,
								@Nullable final DeviceConfig deviceConfig) {
		return new MockDeviceAdapter(deviceType, devicePort, deviceConfiguration, deviceConfig, schedulerService);
	}
}
