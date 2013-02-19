package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class RemoteDeviceConfigDBModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DeviceConfigDB.class).to(RemoteDeviceConfigDB.class).in(Scopes.SINGLETON);
	}
}
