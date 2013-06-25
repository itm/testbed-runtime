package de.uniluebeck.itm.tr.snaa.dummy;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.snaa.SNAA;

public class DummySNAAModule extends PrivateModule {

	private final SNAAConfig snaaConfig;

	public DummySNAAModule(final SNAAConfig snaaConfig) {
		this.snaaConfig = snaaConfig;
	}

	@Override
	protected void configure() {

		requireBinding(SNAAConfig.class);
		requireBinding(ServicePublisher.class);

		bind(DummySNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(DummySNAA.class);
		bind(SNAAService.class).to(DummySNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}
}
