package de.uniluebeck.itm.tr.federator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.federator.iwsn.IWSNFederatorServiceConfig;
import de.uniluebeck.itm.tr.federator.iwsn.IWSNFederatorServiceModule;
import de.uniluebeck.itm.tr.federator.rs.RSFederatorServiceConfig;
import de.uniluebeck.itm.tr.federator.rs.RSFederatorServiceModule;
import de.uniluebeck.itm.tr.federator.snaa.SNAAFederatorServiceModule;
import de.uniluebeck.itm.tr.federator.snaa.SNAAFederatorServiceConfig;
import de.uniluebeck.itm.tr.common.PreconditionsModule;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.util.Providers.of;

public class FederatorServerModule extends AbstractModule {

	private final IWSNFederatorServiceConfig iwsnFederatorServiceConfig;

	private final RSFederatorServiceConfig rsFederatorServiceConfig;

	private final SNAAFederatorServiceConfig snaaFederatorServiceConfig;

	private final FederatorServerConfig federatorServerConfig;

	public FederatorServerModule(final FederatorServerConfig federatorServerConfig,
								 final IWSNFederatorServiceConfig iwsnFederatorServiceConfig,
								 final RSFederatorServiceConfig rsFederatorServiceConfig,
								 final SNAAFederatorServiceConfig snaaFederatorServiceConfig) {
		this.federatorServerConfig = checkNotNull(federatorServerConfig);
		this.iwsnFederatorServiceConfig = checkNotNull(iwsnFederatorServiceConfig);
		this.rsFederatorServiceConfig = checkNotNull(rsFederatorServiceConfig);
		this.snaaFederatorServiceConfig = checkNotNull(snaaFederatorServiceConfig);
	}

	@Override
	protected void configure() {

		bind(IWSNFederatorServiceConfig.class).toProvider(of(iwsnFederatorServiceConfig));
		bind(RSFederatorServiceConfig.class).toProvider(of(rsFederatorServiceConfig));
		bind(SNAAFederatorServiceConfig.class).toProvider(of(snaaFederatorServiceConfig));

		install(new PreconditionsModule());
		install(new ServicePublisherCxfModule());

		install(new IWSNFederatorServiceModule());
		install(new RSFederatorServiceModule());
		install(new SNAAFederatorServiceModule(snaaFederatorServiceConfig));

		install(new FederatorServiceModule());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(federatorServerConfig.getPort(), federatorServerConfig.getShiroIni()));
	}
}
