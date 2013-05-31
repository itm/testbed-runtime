package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class ShibbolethAuthenticatorModule extends AbstractModule {

	private final ShibbolethAuthenticatorConfig config;

	public ShibbolethAuthenticatorModule(final ShibbolethAuthenticatorConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		final Module authenticatorModule = new FactoryModuleBuilder()
				.implement(ShibbolethAuthenticator.class, ShibbolethAuthenticatorImpl.class)
				.build(ShibbolethAuthenticatorFactory.class);
		install(authenticatorModule);
		bind(ShibbolethAuthenticatorConfig.class).toInstance(config);
		bind(ShibbolethAuthenticator.class).to(ShibbolethAuthenticatorImpl.class);
	}
}
