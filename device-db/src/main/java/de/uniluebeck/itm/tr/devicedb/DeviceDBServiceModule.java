package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;

public class DeviceDBServiceModule extends PrivateModule {

	private final DeviceDBConfig deviceDBConfig;

	public DeviceDBServiceModule(final DeviceDBConfig deviceDBConfig) {
		this.deviceDBConfig = deviceDBConfig;
	}

	@Override
	protected void configure() {

		requireBinding(DeviceDBConfig.class);

		switch (deviceDBConfig.getDeviceDBType()) {
			case IN_MEMORY:
				install(new DeviceDBInMemoryModule());
				break;
			case JPA:
				install(new DeviceDBJpaModule(deviceDBConfig.getDeviceDBJPAProperties()));
				break;
			case REMOTE:
				install(new RemoteDeviceDBModule(deviceDBConfig));
				break;
			default:
				throw new IllegalArgumentException(
						"Unknown DeviceDB type \"" + deviceDBConfig.getDeviceDBType().toString() + "\""
				);
		}

		expose(DeviceDBService.class);
	}
}
