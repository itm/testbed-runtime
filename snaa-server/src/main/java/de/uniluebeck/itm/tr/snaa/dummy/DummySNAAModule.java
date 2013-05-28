package de.uniluebeck.itm.tr.snaa.dummy;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.snaa.SNAA;

public class DummySNAAModule extends PrivateModule {

	private final SNAAConfig config;

	public DummySNAAModule(final SNAAConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {

		requireBinding(ServicePublisher.class);

		bind(SNAAConfig.class).toInstance(config);

		bind(DummySNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(DummySNAA.class);
		bind(SNAAService.class).to(DummySNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}
}
