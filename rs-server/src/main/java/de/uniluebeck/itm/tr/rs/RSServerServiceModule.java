package de.uniluebeck.itm.tr.rs;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Provides;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.EndpointManagerImpl;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.SmServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.inject.util.Providers.of;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class RSServerServiceModule extends RSServiceModule {

	private final RSServerConfig rsServerConfig;

	public RSServerServiceModule(final CommonConfig commonConfig,
								 final RSServiceConfig rsServiceConfig,
								 final RSServerConfig rsServerConfig) {
		super(commonConfig, rsServiceConfig);
		this.rsServerConfig = rsServerConfig;
	}

	@Override
	protected void configure() {

		bind(RSServerConfig.class).toProvider(of(rsServerConfig));

		install(new ServicePublisherCxfModule());
		bind(ServedNodeUrnsProvider.class).to(SmServedNodeUrnsProvider.class);
		super.configure();
	}

	@Provides
	TimeLimiter provideTimeLimiter() {
		return new SimpleTimeLimiter(getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool()));
	}

	@Provides
	ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory,
											 final CommonConfig config) {
		return servicePublisherFactory.create(new ServicePublisherConfig(config.getPort()));
	}

	@Provides
	EndpointManager provideEndpointManager(final RSServerConfig config) {
		return new EndpointManagerImpl(
				null,
				config.getSnaaEndpointUri(),
				config.getSmEndpointUri(),
				null
		);
	}

	@Provides
	SessionManagement provideSessionManagement(final RSServerConfig config) {
		return WisebedServiceHelper.getSessionManagementService(config.getSmEndpointUri().toString());
	}

	@Provides
	SNAA provideSnaa(final RSServerConfig config) {
		return WisebedServiceHelper.getSNAAService(config.getSnaaEndpointUri().toString());
	}
}
