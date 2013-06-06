package de.uniluebeck.itm.tr.snaa;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.snaa.dummy.DummySNAAModule;
import de.uniluebeck.itm.tr.snaa.jaas.JAASSNAAModule;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAModule;
import de.uniluebeck.itm.tr.snaa.shiro.JpaModule;
import de.uniluebeck.itm.tr.snaa.shiro.ShiroSNAAModule;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static com.google.common.base.Throwables.propagate;

public class SNAAServerModule extends AbstractModule {

	private final SNAAConfig config;

	public SNAAServerModule(final SNAAConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {

		install(new ServicePublisherCxfModule());

		switch (config.getSnaaType()) {
			case DUMMY:
				install(new DummySNAAModule(config));
				break;
			case JAAS:
				install(new JAASSNAAModule(config));
				break;
			case SHIBBOLETH:
				install(new ShibbolethSNAAModule(config));
				break;
			case SHIRO:
				install(new JpaModule("ShiroSNAA", loadHibernateProperties(config)));
				install(new ShiroSNAAModule(config));
				break;
			default:
				throw new IllegalArgumentException("Unknown authentication type " + config.getSnaaType());
		}
	}

	private Properties loadHibernateProperties(final SNAAConfig config) {
		final String hibernatePropertiesFileName =
				config.getSnaaProperties().getProperty(SNAAProperties.SHIRO_HIBERNATE_PROPERTIES);
		final File hibernatePropertiesFile = new File(hibernatePropertiesFileName);

		final Properties hibernateProperties = new Properties();
		try {
			hibernateProperties.load(new FileReader(hibernatePropertiesFile));
		} catch (IOException e) {
			throw propagate(e);
		}
		return hibernateProperties;
	}

	@Provides
	@Singleton
	public ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory) {
		return servicePublisherFactory.create(new ServicePublisherConfig(config.getPort()));
	}
}
