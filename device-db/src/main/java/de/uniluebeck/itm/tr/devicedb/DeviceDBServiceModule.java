package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class DeviceDBServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder()
				.implement(DeviceDBService.class, DeviceDBServiceImpl.class)
				.build(DeviceDBServiceFactory.class)
		);
	}
}
