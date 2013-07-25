package de.uniluebeck.itm.tr.common.plugins;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class PluginContainerModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder().implement(PluginContainer.class, PluginContainerImpl.class)
				.build(PluginContainerFactory.class)
		);
	}
}
