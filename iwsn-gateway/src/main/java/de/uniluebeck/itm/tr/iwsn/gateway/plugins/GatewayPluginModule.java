package de.uniluebeck.itm.tr.iwsn.gateway.plugins;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.common.plugins.PluginContainerModule;

public class GatewayPluginModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new PluginContainerModule());
		bind(GatewayPluginService.class).to(GatewayPluginServiceImpl.class).in(Scopes.SINGLETON);
	}
}
