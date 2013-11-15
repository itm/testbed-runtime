package de.uniluebeck.itm.tr.federator.rs;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.PreconditionsModule;

import static com.google.inject.util.Providers.of;

public class RSFederatorServerModule extends AbstractModule {

	private final RSFederatorServerConfig rsFederatorServerConfig;

	private final RSFederatorServiceConfig rsFederatorServiceConfig;

	public RSFederatorServerModule(final RSFederatorServerConfig rsFederatorServerConfig,
								   final RSFederatorServiceConfig rsFederatorServiceConfig) {
		this.rsFederatorServerConfig = rsFederatorServerConfig;
		this.rsFederatorServiceConfig = rsFederatorServiceConfig;
	}

	@Override
	protected void configure() {

		bind(RSFederatorServerConfig.class).toProvider(of(rsFederatorServerConfig));
		bind(RSFederatorServiceConfig.class).toProvider(of(rsFederatorServiceConfig));

		install(new PreconditionsModule());
		install(new ServicePublisherCxfModule());
		install(new RSFederatorServiceModule());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory, final RSFederatorServerConfig config) {
		return servicePublisherFactory.create(new ServicePublisherConfig(config.getPort(), config.getShiroIni()));
	}
}
