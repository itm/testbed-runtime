package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.AbstractModule;

public class IWSNOverlayManagerModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(IWSNOverlayManagerFactory.class).to(IWSNOverlayManagerFactoryImpl.class);
	}
}
