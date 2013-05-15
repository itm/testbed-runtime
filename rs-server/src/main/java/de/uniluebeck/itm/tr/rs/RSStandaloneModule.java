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
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class RSStandaloneModule extends RSModule {

	private final RSStandaloneConfig config;

	public RSStandaloneModule(final RSStandaloneConfig config) {
		super(config);
		this.config = config;
	}

	@Override
	protected void configure() {
		install(new ServicePublisherCxfModule());
		bind(ServedNodeUrnsProvider.class).to(SmServedNodeUrnsProvider.class);
		super.configure();
	}

	@Provides
	TimeLimiter provideTimeLimiter() {
		return new SimpleTimeLimiter(getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool()));
	}

	@Provides
	ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory) {
		return servicePublisherFactory.create(new ServicePublisherConfig(config.getPort()));
	}

	@Provides
	EndpointManager provideEndpointManager() {
		return new EndpointManagerImpl(
				null,
				config.getSnaaEndpointUri(),
				config.getSmEndpointUri(),
				null
		);
	}

	@Provides
	SessionManagement provideSessionManagement() {
		return WisebedServiceHelper.getSessionManagementService(config.getSmEndpointUri().toString());
	}

	@Provides
	SNAA provideSnaa() {
		return WisebedServiceHelper.getSNAAService(config.getSnaaEndpointUri().toString());
	}
}
