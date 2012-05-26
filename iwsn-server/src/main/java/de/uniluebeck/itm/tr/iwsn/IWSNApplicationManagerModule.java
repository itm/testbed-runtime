package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.AbstractModule;

public class IWSNApplicationManagerModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(IWSNApplicationManagerFactory.class).to(IWSNApplicationManagerFactoryImpl.class);
	}
}
