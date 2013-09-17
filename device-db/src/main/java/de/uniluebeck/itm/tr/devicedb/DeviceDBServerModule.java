package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Providers;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceModule;

public class DeviceDBServerModule extends AbstractModule {

	private final CommonConfig commonConfig;

	private final DeviceDBConfig deviceDBConfig;

	private final WisemlProviderConfig wisemlProviderConfig;

	public DeviceDBServerModule(final CommonConfig commonConfig,
								final DeviceDBConfig deviceDBConfig,
								final WisemlProviderConfig wisemlProviderConfig) {
		this.commonConfig = commonConfig;
		this.deviceDBConfig = deviceDBConfig;
		this.wisemlProviderConfig = wisemlProviderConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(Providers.of(commonConfig));
		bind(DeviceDBConfig.class).toProvider(Providers.of(deviceDBConfig));
		bind(WisemlProviderConfig.class).toProvider(Providers.of(wisemlProviderConfig));

		install(new SchedulerServiceModule());
		install(new ServicePublisherCxfModule());
		install(new DeviceDBServiceModule(deviceDBConfig));
		install(new DeviceDBRestServiceModule());
	}

	@Provides
	@Singleton
	SchedulerService provideSchedulerService(SchedulerServiceFactory factory) {
		return factory.create(-1, "DeviceDBServer");
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory, final CommonConfig commonConfig) {
		return factory.create(new ServicePublisherConfig(commonConfig.getPort()));
	}
}
