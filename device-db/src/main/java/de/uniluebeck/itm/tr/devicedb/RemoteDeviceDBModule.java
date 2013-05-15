package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

public class RemoteDeviceDBModule extends PrivateModule {

	private final RemoteDeviceDBConfig config;

	public RemoteDeviceDBModule(final RemoteDeviceDBConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		bind(RemoteDeviceDBConfig.class).toInstance(config);
		bind(DeviceDB.class).to(RemoteDeviceDB.class).in(Scopes.SINGLETON);
		expose(DeviceDB.class);
	}
}
