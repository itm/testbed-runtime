package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

public class RemoteDeviceDBModule extends PrivateModule {

	@Override
	protected void configure() {
		requireBinding(RemoteDeviceDBConfig.class);
		bind(DeviceDB.class).to(RemoteDeviceDB.class).in(Scopes.SINGLETON);
		expose(DeviceDB.class);
	}
}
