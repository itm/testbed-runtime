package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;

import static com.google.inject.util.Providers.of;

public class IWSNFederatorServerModule extends AbstractModule {

	private final IWSNFederatorServiceConfig iwsnFederatorServiceConfig;

	private final IWSNFederatorServerConfig iwsnFederatorServerConfig;

	public IWSNFederatorServerModule(final IWSNFederatorServerConfig iwsnFederatorServerConfig,
									 final IWSNFederatorServiceConfig iwsnFederatorServiceConfig) {
		this.iwsnFederatorServiceConfig = iwsnFederatorServiceConfig;
		this.iwsnFederatorServerConfig = iwsnFederatorServerConfig;
	}

	@Override
	protected void configure() {

		bind(IWSNFederatorServerConfig.class).toProvider(of(iwsnFederatorServerConfig));
		bind(IWSNFederatorServiceConfig.class).toProvider(of(iwsnFederatorServiceConfig));

		install(new ServicePublisherCxfModule());
		install(new IWSNFederatorServiceModule());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(iwsnFederatorServerConfig.getPort(), iwsnFederatorServerConfig.getShiroIni()));
	}
}
