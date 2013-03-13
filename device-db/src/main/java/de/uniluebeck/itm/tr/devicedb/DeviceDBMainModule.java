package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.ServicePublisherJettyMetroJerseyModule;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static com.google.common.base.Throwables.propagate;

public class DeviceDBMainModule extends AbstractModule {

	private final DeviceDBMainConfig config;

	public DeviceDBMainModule(final DeviceDBMainConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		install(new ServicePublisherJettyMetroJerseyModule());
		install(new DeviceDBJpaModule(readProperties(config.dbPropertiesFile)));
		install(new DeviceDBServiceModule());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(
				config.port,
				this.getClass().getResource("/").toString()
		));
	}

	private Properties readProperties(final File dbPropertiesFile) {
		try {
			final Properties properties = new Properties();
			properties.load(new FileReader(dbPropertiesFile));
			return properties;
		} catch (IOException e) {
			throw propagate(e);
		}
	}
}
