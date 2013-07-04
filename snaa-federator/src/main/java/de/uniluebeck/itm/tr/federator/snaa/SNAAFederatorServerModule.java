package de.uniluebeck.itm.tr.federator.snaa;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;

import static com.google.inject.util.Providers.of;

public class SNAAFederatorServerModule extends AbstractModule {

	private final SNAAFederatorServerConfig snaaFederatorServerConfig;

	private final SNAAFederatorServiceConfig snaaFederatorServiceConfig;

	public SNAAFederatorServerModule(final SNAAFederatorServerConfig snaaFederatorServerConfig,
									 final SNAAFederatorServiceConfig snaaFederatorServiceConfig) {
		this.snaaFederatorServerConfig = snaaFederatorServerConfig;
		this.snaaFederatorServiceConfig = snaaFederatorServiceConfig;
	}

	@Override
	protected void configure() {

		bind(SNAAFederatorServerConfig.class).toProvider(of(snaaFederatorServerConfig));
		bind(SNAAFederatorServiceConfig.class).toProvider(of(snaaFederatorServiceConfig));

		install(new SNAAFederatorServiceModule(snaaFederatorServiceConfig));
		install(new ServicePublisherCxfModule());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory) {
		return servicePublisherFactory.create(new ServicePublisherConfig(snaaFederatorServerConfig.getPort()));
	}
}
