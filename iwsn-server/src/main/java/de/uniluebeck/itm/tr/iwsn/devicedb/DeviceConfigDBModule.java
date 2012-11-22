package de.uniluebeck.itm.tr.iwsn.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class DeviceConfigDBModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DeviceConfigDB.class).to(DeviceConfigDBImpl.class).in(Singleton.class);
	}
}
