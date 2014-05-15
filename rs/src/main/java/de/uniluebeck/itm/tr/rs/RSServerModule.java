package de.uniluebeck.itm.tr.rs;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Provides;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.SmServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.inject.util.Providers.of;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class RSServerModule extends RSServiceModule {

	private final RSServerConfig rsServerConfig;

	public RSServerModule(final CommonConfig commonConfig,
						  final RSServiceConfig rsServiceConfig,
						  final RSServerConfig rsServerConfig) {
		super(commonConfig, rsServiceConfig);
		this.rsServerConfig = rsServerConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(of(commonConfig));
		bind(RSServerConfig.class).toProvider(of(rsServerConfig));
		bind(RSServiceConfig.class).toProvider(of(rsServiceConfig));

		install(new ServicePublisherCxfModule());
		bind(ServedNodeUrnsProvider.class).to(SmServedNodeUrnsProvider.class);
		super.configure();
		expose(ServicePublisher.class);
	}

	@Provides
	TimeLimiter provideTimeLimiter() {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("TimeLimiter %d").build();
		return new SimpleTimeLimiter(
				getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool(threadFactory))
		);
	}

	@Provides
	ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory,
											 final CommonConfig config) {
		return servicePublisherFactory.create(new ServicePublisherConfig(config.getPort()));
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
