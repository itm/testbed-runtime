package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;

public class ShibbolethAuthenticatorModule extends AbstractModule {

	@Override
	protected void configure() {
		requireBinding(SNAAServiceConfig.class);
		bind(ShibbolethAuthenticator.class).to(ShibbolethAuthenticatorImpl.class);
	}
}
