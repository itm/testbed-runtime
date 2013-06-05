package de.uniluebeck.itm.tr.snaa.jaas;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import edu.internet2.middleware.shibboleth.jaas.htpasswd.HtpasswdLoginModule;
import eu.wisebed.api.v3.snaa.SNAA;

import javax.security.auth.spi.LoginModule;

import static de.uniluebeck.itm.tr.snaa.SNAAProperties.CONTEXT_PATH;
import static de.uniluebeck.itm.tr.snaa.SNAAProperties.JAAS_LOGINMODULE;

public class JAASSNAAModule extends PrivateModule {

	private final SNAAConfig config;

	public JAASSNAAModule(final SNAAConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {

		requireBinding(ServicePublisher.class);

		bind(String.class).annotatedWith(Names.named(CONTEXT_PATH)).toInstance(config.getSnaaContextPath());
		bind(SNAAConfig.class).toInstance(config);

		final JAASSNAALoginModule loginModule = JAASSNAALoginModule.valueOf(
				config.getSnaaProperties().getProperty(JAAS_LOGINMODULE)
		);

		switch (loginModule) {
			case ALWAYS_TRUE:
				bind(LoginModule.class).to(AlwaysTrueLoginModule.class).in(Scopes.SINGLETON);
				break;
			case HTPASSWD:
				bind(LoginModule.class).to(HtpasswdLoginModule.class);
				break;
		}

		bind(JAASSNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(JAASSNAA.class);
		bind(SNAAService.class).to(JAASSNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}
}
