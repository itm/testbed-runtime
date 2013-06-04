package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;

import java.util.Properties;

public class ShibbolethSNAAModule extends AbstractModule {

	private final Properties snaaProperties;

	private final String snaaContextPath;

	public ShibbolethSNAAModule(final SNAAConfig config) {
		this.snaaProperties = config.getSnaaProperties();
		this.snaaContextPath = config.getSnaaContextPath();
	}

	public ShibbolethSNAAModule(final Properties snaaProperties, final String snaaContextPath) {
		this.snaaProperties = snaaProperties;
		this.snaaContextPath = snaaContextPath;
	}

	@Override
	protected void configure() {

		requireBinding(ServedNodeUrnPrefixesProvider.class);

		final ShibbolethAuthenticatorConfig authenticatorConfig =
				new ShibbolethAuthenticatorConfig(snaaProperties);
		install(new ShibbolethAuthenticatorModule(authenticatorConfig));

		bind(String.class).annotatedWith(Names.named("snaaContextPath")).toInstance(snaaContextPath);
		bind(SNAAService.class).to(ShibbolethSNAA.class).in(Scopes.SINGLETON);
	}
}
