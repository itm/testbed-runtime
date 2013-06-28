package de.uniluebeck.itm.tr.snaa.jaas;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class JAASSNAAModule extends PrivateModule {

	private final CommonConfig commonConfig;

	private final SNAAConfig snaaConfig;

	public JAASSNAAModule(final CommonConfig commonConfig, final SNAAConfig snaaConfig) {
		this.commonConfig = commonConfig;
		this.snaaConfig = snaaConfig;
	}

	@Override
	protected void configure() {

		requireBinding(CommonConfig.class);
		requireBinding(SNAAConfig.class);
		requireBinding(ServicePublisher.class);

		bind(LoginContextFactory.class).to(LoginContextFactoryImpl.class);
		bind(JAASSNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(JAASSNAA.class);
		bind(SNAAService.class).to(JAASSNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}

	@Provides
	ServedNodeUrnPrefixesProvider provideServedNodeUrnPrefixesProvider(final CommonConfig config) {
		return new ServedNodeUrnPrefixesProvider() {
			@Override
			public Set<NodeUrnPrefix> get() {
				return newHashSet(config.getUrnPrefix());
			}
		};
	}
}
