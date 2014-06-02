package de.uniluebeck.itm.tr.federator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.EndpointManagerImpl;
import de.uniluebeck.itm.tr.common.PreconditionsModule;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.federator.iwsn.IWSNFederatorServiceConfig;
import de.uniluebeck.itm.tr.federator.iwsn.IWSNFederatorServiceModule;
import de.uniluebeck.itm.tr.federator.rs.RSFederatorServiceConfig;
import de.uniluebeck.itm.tr.federator.rs.RSFederatorServiceModule;
import de.uniluebeck.itm.tr.federator.snaa.SNAAFederatorServiceConfig;
import de.uniluebeck.itm.tr.federator.snaa.SNAAFederatorServiceModule;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerModule;
import de.uniluebeck.itm.tr.iwsn.portal.WiseGuiServiceConfig;
import de.uniluebeck.itm.tr.iwsn.portal.WiseGuiServiceModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiModule;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceModule;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.util.Providers.of;

public class FederatorServerModule extends AbstractModule {

	private final IWSNFederatorServiceConfig iwsnFederatorServiceConfig;

	private final RSFederatorServiceConfig rsFederatorServiceConfig;

	private final SNAAFederatorServiceConfig snaaFederatorServiceConfig;

	private final FederatorServerConfig federatorServerConfig;

	private final WiseGuiServiceConfig wiseGuiServiceConfig;

	public FederatorServerModule(final FederatorServerConfig federatorServerConfig,
								 final IWSNFederatorServiceConfig iwsnFederatorServiceConfig,
								 final RSFederatorServiceConfig rsFederatorServiceConfig,
								 final SNAAFederatorServiceConfig snaaFederatorServiceConfig,
								 final WiseGuiServiceConfig wiseGuiServiceConfig) {
		this.wiseGuiServiceConfig = wiseGuiServiceConfig;
		this.federatorServerConfig = checkNotNull(federatorServerConfig);
		this.iwsnFederatorServiceConfig = checkNotNull(iwsnFederatorServiceConfig);
		this.rsFederatorServiceConfig = checkNotNull(rsFederatorServiceConfig);
		this.snaaFederatorServiceConfig = checkNotNull(snaaFederatorServiceConfig);
	}

	@Override
	protected void configure() {

		bind(FederatorServerConfig.class).toProvider(of(federatorServerConfig));
		bind(IWSNFederatorServiceConfig.class).toProvider(of(iwsnFederatorServiceConfig));
		bind(RSFederatorServiceConfig.class).toProvider(of(rsFederatorServiceConfig));
		bind(SNAAFederatorServiceConfig.class).toProvider(of(snaaFederatorServiceConfig));
		bind(WiseGuiServiceConfig.class).toProvider(of(wiseGuiServiceConfig));

		bind(EndpointManager.class).to(EndpointManagerImpl.class).in(Scopes.SINGLETON);

		install(new PreconditionsModule());
		install(new ServicePublisherCxfModule());
		install(new IWSNFederatorServiceModule());
		install(new RSFederatorServiceModule());
		install(new SNAAFederatorServiceModule(snaaFederatorServiceConfig));
		install(new FederatorServiceModule());
		install(new WiseGuiServiceModule());
		install(new SchedulerServiceModule());
		install(new ResponseTrackerModule());
		install(new RestApiModule(true));
	}

	@Provides
	@Singleton
	SchedulerService provideSchedulerService(final SchedulerServiceFactory factory) {
		return factory.create(1, "FederatorServer");
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(federatorServerConfig.getFederatorPort()));
	}

	@Provides
	@Singleton
	CommonConfig provideCommonConfig(final FederatorServerConfig config) {
		final CommonConfig commonConfig = new CommonConfig();
		commonConfig.setPort(config.getFederatorPort());
		commonConfig.setHostname(config.getFederatorHostname());
		return commonConfig;
	}
}
