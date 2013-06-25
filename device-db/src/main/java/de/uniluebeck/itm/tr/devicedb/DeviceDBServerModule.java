package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Providers;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.config.CommonConfig;

public class DeviceDBServerModule extends PrivateModule {

	private final CommonConfig commonConfig;

	private final DeviceDBConfig deviceDBConfig;

	public DeviceDBServerModule(final CommonConfig commonConfig, final DeviceDBConfig deviceDBConfig) {
		this.commonConfig = commonConfig;
		this.deviceDBConfig = deviceDBConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(Providers.of(commonConfig));
		bind(DeviceDBConfig.class).toProvider(Providers.of(deviceDBConfig));

		install(new DeviceDBServiceModule(deviceDBConfig));
		install(new ServicePublisherCxfModule());
		install(new DeviceDBRestServiceModule());

		expose(DeviceDBService.class);
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory, final CommonConfig commonConfig) {
		return factory.create(new ServicePublisherConfig(commonConfig.getPort()));
	}
}
