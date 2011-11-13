package de.uniluebeck.itm.gtr;

import com.google.inject.AbstractModule;

public class LocalNodeNameManagerModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(LocalNodeNameManager.class).to(LocalNodeNameManagerImpl.class);
	}
}
