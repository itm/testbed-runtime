package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class DeviceDBInMemoryModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DeviceDB.class).to(DeviceDBInMemory.class).in(Scopes.SINGLETON);
	}
}
