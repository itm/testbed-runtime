package de.uniluebeck.itm.tr.iwsn.overlay;

import com.google.inject.AbstractModule;

public class LocalNodeNameManagerModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(LocalNodeNameManager.class).to(LocalNodeNameManagerImpl.class);
	}
}
