package de.uniluebeck.itm.tr.federator.utils;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

public class FederationManagerModule extends PrivateModule {

	@Override
	protected void configure() {
		bind(FederationManagerFactory.class).to(FederationManagerFactoryImpl.class).in(Scopes.SINGLETON);
		expose(FederationManagerFactory.class);
	}
}
