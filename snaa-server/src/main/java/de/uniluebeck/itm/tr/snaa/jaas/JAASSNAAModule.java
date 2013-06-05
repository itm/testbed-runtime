package de.uniluebeck.itm.tr.snaa.jaas;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.Properties;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.snaa.SNAAProperties.CONTEXT_PATH;
import static de.uniluebeck.itm.tr.snaa.SNAAProperties.JAAS_CONFIG;
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

		final Properties snaaProperties = config.getSnaaProperties();
		checkNotNull(snaaProperties, "Please provide an SNAA properties file!");

		final String jaasConfig = snaaProperties.getProperty(JAAS_CONFIG);
		checkNotNull(jaasConfig,
				"Please provide the JAAS config file (key \"" + JAAS_CONFIG + "\") in the SNAA properties file!"
		);
		bind(String.class).annotatedWith(Names.named(JAAS_CONFIG)).toInstance(jaasConfig);
		System.setProperty("java.security.auth.login.config", jaasConfig);

		final String jaasLoginModule = snaaProperties.getProperty(JAAS_LOGINMODULE);
		bind(String.class).annotatedWith(Names.named(JAAS_LOGINMODULE)).toInstance(jaasLoginModule);
		checkNotNull(jaasLoginModule,
				"Please provide the JAAS login module (key \"" + JAAS_LOGINMODULE + "\") in the SNAA properties file!"
		);

		bind(LoginContextFactory.class).to(LoginContextFactoryImpl.class);
		bind(JAASSNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(JAASSNAA.class);
		bind(SNAAService.class).to(JAASSNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}

	@Provides
	ServedNodeUrnPrefixesProvider provideServedNodeUrnPrefixesProvider() {
		return new ServedNodeUrnPrefixesProvider() {
			@Override
			public Set<NodeUrnPrefix> get() {
				return newHashSet(config.getUrnPrefix());
			}
		};
	}
}
