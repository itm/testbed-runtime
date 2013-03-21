package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;

public class DeviceDBMainModule extends AbstractModule {

	private final DeviceDBMainConfig config;

	public DeviceDBMainModule(final DeviceDBMainConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {

		bind(DeviceDBRestResource.class).to(DeviceDBRestResourceImpl.class).in(Scopes.SINGLETON);

		install(new ServicePublisherCxfModule());
		install(new DeviceDBJpaModule(config.dbProperties));
		install(new DeviceDBServiceModule());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(config.port));
	}
}
