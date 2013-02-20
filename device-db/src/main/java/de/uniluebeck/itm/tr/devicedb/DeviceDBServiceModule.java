package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherJettyMetroJerseyModule;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static com.google.common.base.Throwables.propagate;

public class DeviceDBServiceModule extends AbstractModule {

	private final DeviceDBServiceConfig config;

	public DeviceDBServiceModule(final DeviceDBServiceConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		bind(DeviceDBServiceConfig.class).toInstance(config);
		install(new ServicePublisherJettyMetroJerseyModule());
		install(new DeviceDBJpaModule(readProperties(config.dbPropertiesFile)));
	}

	@Provides
	ServicePublisherConfig provideServicePublisherConfig() {
		return new ServicePublisherConfig(config.port);
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
