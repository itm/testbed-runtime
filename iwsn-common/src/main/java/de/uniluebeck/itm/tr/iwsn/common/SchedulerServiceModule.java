package de.uniluebeck.itm.tr.iwsn.common;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class SchedulerServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		final Module factoryModule = new FactoryModuleBuilder()
				.implement(SchedulerService.class, SchedulerServiceImpl.class)
				.build(SchedulerServiceFactory.class);
		install(factoryModule);
	}
}
