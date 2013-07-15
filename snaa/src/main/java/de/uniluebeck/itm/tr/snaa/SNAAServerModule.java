package de.uniluebeck.itm.tr.snaa;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfigServedNodeUrnPrefixesProvider;

import static com.google.inject.util.Providers.of;

public class SNAAServerModule extends AbstractModule {

	private final CommonConfig commonConfig;

	private final SNAAServiceConfig snaaServiceConfig;

	public SNAAServerModule(final CommonConfig commonConfig, final SNAAServiceConfig snaaServiceConfig) {
		this.commonConfig = commonConfig;
		this.snaaServiceConfig = snaaServiceConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(of(commonConfig));
		bind(SNAAServiceConfig.class).toProvider(of(snaaServiceConfig));
		bind(ServedNodeUrnPrefixesProvider.class).to(CommonConfigServedNodeUrnPrefixesProvider.class);

		install(new ServicePublisherCxfModule());
		install(new SNAAServiceModule(commonConfig, snaaServiceConfig));
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final CommonConfig config, final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(config.getPort()));
	}
}
