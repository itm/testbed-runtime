package de.uniluebeck.itm.tr.common;

import com.google.inject.AbstractModule;

public class PreconditionsModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(PreconditionsFactory.class).to(PreconditionsFactoryImpl.class);
	}
}
