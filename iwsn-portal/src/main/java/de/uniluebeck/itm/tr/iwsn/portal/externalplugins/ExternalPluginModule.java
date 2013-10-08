package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;

public class ExternalPluginModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(ExternalPluginServiceConfig.class);
		requireBinding(PortalEventBus.class);
		requireBinding(NettyServerFactory.class);

		bind(ExternalPluginService.class).to(ExternalPluginServiceImpl.class).in(Scopes.SINGLETON);

		expose(ExternalPluginService.class);
	}
}
