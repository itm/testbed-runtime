package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;

import static com.google.inject.util.Providers.of;

public class ShibbolethAuthenticatorModule extends AbstractModule {

	private final SNAAConfig snaaConfig;

	public ShibbolethAuthenticatorModule(final SNAAConfig snaaConfig) {
		this.snaaConfig = snaaConfig;
	}

	@Override
	protected void configure() {
		bind(SNAAConfig.class).toProvider(of(snaaConfig));
		bind(ShibbolethAuthenticator.class).to(ShibbolethAuthenticatorImpl.class);
	}
}
