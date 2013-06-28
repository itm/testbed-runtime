package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;

public class ShibbolethAuthenticatorModule extends AbstractModule {

	@Override
	protected void configure() {
		requireBinding(SNAAConfig.class);
		bind(ShibbolethAuthenticator.class).to(ShibbolethAuthenticatorImpl.class);
	}
}
