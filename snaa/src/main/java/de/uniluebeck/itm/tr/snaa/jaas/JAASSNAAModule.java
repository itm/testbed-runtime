package de.uniluebeck.itm.tr.snaa.jaas;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfigServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import eu.wisebed.api.v3.snaa.SNAA;

public class JAASSNAAModule extends PrivateModule {

	private final CommonConfig commonConfig;

	private final SNAAServiceConfig snaaServiceConfig;

	public JAASSNAAModule(final CommonConfig commonConfig, final SNAAServiceConfig snaaServiceConfig) {
		this.commonConfig = commonConfig;
		this.snaaServiceConfig = snaaServiceConfig;
	}

	@Override
	protected void configure() {

		requireBinding(CommonConfig.class);
		requireBinding(SNAAServiceConfig.class);
		requireBinding(ServicePublisher.class);

		bind(ServedNodeUrnPrefixesProvider.class).to(CommonConfigServedNodeUrnPrefixesProvider.class);
		bind(LoginContextFactory.class).to(LoginContextFactoryImpl.class);
		bind(JAASSNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(JAASSNAA.class);
		bind(SNAAService.class).to(JAASSNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}
}
