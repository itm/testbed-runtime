package de.uniluebeck.itm.tr.iwsn.portal.plugins;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.common.plugins.PluginContainerModule;

public class PortalPluginModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new PluginContainerModule());
		bind(PortalPluginService.class).to(PortalPluginServiceImpl.class).in(Scopes.SINGLETON);
	}
}
