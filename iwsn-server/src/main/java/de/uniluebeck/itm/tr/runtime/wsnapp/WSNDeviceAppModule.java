package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class WSNDeviceAppModule extends AbstractModule {

	@Override
	protected void configure() {

		install(new FactoryModuleBuilder()
				.implement(WSNDeviceApp.class, WSNDeviceAppImpl.class)
				.build(WSNDeviceAppGuiceFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(WSNDeviceAppConnector.class, WSNDeviceAppConnectorImpl.class)
				.build(WSNDeviceAppConnectorFactory.class)
		);
	}
}
