package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.gtr.TestbedRuntimeModule;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserverModule;

public class IWSNModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new DOMObserverModule());
		install(new TestbedRuntimeModule());
		install(new IWSNOverlayManagerModule());
		install(new IWSNApplicationManagerModule());
		bind(IWSNFactory.class).to(IWSNFactoryImpl.class);
	}
}
