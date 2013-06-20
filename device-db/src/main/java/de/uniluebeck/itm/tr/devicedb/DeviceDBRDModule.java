package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class DeviceDBRDModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DeviceDB.class).to(DeviceDBRD.class).in(Scopes.SINGLETON);
	}
}
