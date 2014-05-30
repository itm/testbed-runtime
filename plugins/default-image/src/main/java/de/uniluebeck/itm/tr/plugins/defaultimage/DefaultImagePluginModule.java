package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.rs.RSHelperModule;

public class DefaultImagePluginModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new RSHelperModule());
		bind(DefaultImagePlugin.class).to(DefaultImagePluginImpl.class).in(Scopes.SINGLETON);
	}

	@Provides
	@Singleton
	public EventBusService provideEventBusService(final PortalEventBus portalEventBus) {
		return portalEventBus;
	}
}
