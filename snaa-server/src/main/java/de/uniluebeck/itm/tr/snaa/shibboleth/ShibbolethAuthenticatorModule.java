package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;

public class ShibbolethAuthenticatorModule extends AbstractModule {

	private final ShibbolethAuthenticatorConfig config;

	public ShibbolethAuthenticatorModule(final ShibbolethAuthenticatorConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		bind(ShibbolethAuthenticatorConfig.class).toInstance(config);
		bind(ShibbolethAuthenticator.class).to(ShibbolethAuthenticatorImpl.class);
	}
}
