package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class WSNServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder()
		.implement(WSNService.class, WSNServiceImpl.class)
		.build(WSNServiceFactory.class)
);
	}
	

}
