package de.uniluebeck.itm.tr.rs;

import com.google.inject.AbstractModule;

public class RSHelperModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(RSHelper.class).to(RSHelperImpl.class);
	}
}
