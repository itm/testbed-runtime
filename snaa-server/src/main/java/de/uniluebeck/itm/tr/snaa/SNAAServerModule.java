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

public class SNAAServerModule extends AbstractModule {

	private final SNAAConfig config;

	public SNAAServerModule(final SNAAConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {

		install(new ServicePublisherCxfModule());

		switch (config.getSnaaAuthenticationType()) {
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
				throw new RuntimeException("Implement me!");
			default:
				throw new IllegalArgumentException("Unknown authentication type " + config.getSnaaAuthenticationType());
		}

		switch (config.getSnaaAuthorizationType()) {
			case ALWAYS_ALLOW:
				throw new RuntimeException("Implement me!");
			case ALWAYS_DENY:
				throw new RuntimeException("Implement me!");
			case ATTRIBUTE_BASED:
				throw new RuntimeException("Implement me!");
			default:
				throw new IllegalArgumentException("Unknown authorization type " + config.getSnaaAuthorizationType());
		}
	}

	@Provides
	@Singleton
	public ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory) {
		return servicePublisherFactory.create(new ServicePublisherConfig(config.getPort()));
	}
}
