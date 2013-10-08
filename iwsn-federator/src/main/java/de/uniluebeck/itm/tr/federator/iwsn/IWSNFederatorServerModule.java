package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.snaa.SNAA;

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
	RS provideRS(final IWSNFederatorServiceConfig config) {
		return WisebedServiceHelper.getRSService(config.getFederatorRsEndpointUri().toString());
	}

	@Provides
	SNAA provideSNAA(final IWSNFederatorServiceConfig config) {
		return WisebedServiceHelper.getSNAAService(config.getFederatorSnaaEndpointUri().toString());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(iwsnFederatorServerConfig.getPort(), iwsnFederatorServerConfig.getShiroIni()));
	}
}
