package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.iwsn.netty.NettyServerModule;

public class PortalModule extends AbstractModule {

	private final PortalConfig portalConfig;

	public PortalModule(final PortalConfig portalConfig) {
		this.portalConfig = portalConfig;
	}

	@Override
	protected void configure() {

		bind(PortalConfig.class).toInstance(portalConfig);
		bind(PortalEventBus.class).to(PortalEventBusImpl.class).in(Singleton.class);

		install(new NettyServerModule());
	}

	@Provides
	EventBus provideEventBus() {
		return new EventBus("PortalEventBus");
	}
}
