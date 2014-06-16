package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.common.WisemlProvider;
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import eu.wisebed.wiseml.Wiseml;

public class DeviceDBServiceModule extends PrivateModule {

	private final DeviceDBConfig deviceDBConfig;

	public DeviceDBServiceModule(final DeviceDBConfig deviceDBConfig) {
		this.deviceDBConfig = deviceDBConfig;
	}

	@Override
	protected void configure() {

		requireBinding(DeviceDBConfig.class);
		requireBinding(WisemlProviderConfig.class);
		requireBinding(EventBusService.class);

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
			case SMARTSANTANDER:
				install(new DeviceDBRDModule());
				break;
			default:
				throw new IllegalArgumentException(
						"Unknown DeviceDB type \"" + deviceDBConfig.getDeviceDBType().toString() + "\""
				);
		}

		bind(WisemlProvider.class).to(DeviceDBWisemlProvider.class);
		bind(Wiseml.class).toProvider(DeviceDBWisemlProvider.class);
		bind(CachedDeviceDBService.class).to(CachedDeviceDBServiceImpl.class);

		expose(WisemlProvider.class);
		expose(Wiseml.class);
		expose(DeviceDBService.class);
		expose(CachedDeviceDBService.class);
	}
}
