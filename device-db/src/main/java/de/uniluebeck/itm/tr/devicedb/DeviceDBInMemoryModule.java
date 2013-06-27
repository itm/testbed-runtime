package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

public class DeviceDBInMemoryModule extends PrivateModule {

	@Override
	protected void configure() {
		bind(DeviceDBService.class).to(DeviceDBInMemory.class).in(Scopes.SINGLETON);
		expose(DeviceDBService.class);
	}
}
