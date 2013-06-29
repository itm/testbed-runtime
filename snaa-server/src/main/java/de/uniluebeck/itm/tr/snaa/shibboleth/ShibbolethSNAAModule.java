package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class ShibbolethSNAAModule extends PrivateModule {

	private final CommonConfig commonConfig;

	private final SNAAServiceConfig snaaServiceConfig;

	public ShibbolethSNAAModule(final CommonConfig commonConfig, final SNAAServiceConfig snaaServiceConfig) {
		this.commonConfig = commonConfig;
		this.snaaServiceConfig = snaaServiceConfig;
	}

	@Override
	protected void configure() {

		requireBinding(CommonConfig.class);
		requireBinding(SNAAServiceConfig.class);

		switch (snaaServiceConfig.getShibbolethAuthorizationType()) {
			case ALWAYS_ALLOW:
				bind(ShibbolethAuthorization.class).to(AlwaysAllowShibbolethAuthorization.class).in(Scopes.SINGLETON);
				break;
			case ALWAYS_DENY:
				bind(ShibbolethAuthorization.class).to(AlwaysDenyShibbolethAuthorization.class).in(Scopes.SINGLETON);
				break;
			case ATTRIBUTE_BASED:
				bind(AttributeBasedShibbolethAuthorizationAttributes.class)
						.toInstance(parseAttributesFromProperties(snaaServiceConfig));
				bind(ShibbolethAuthorization.class).to(AttributeBasedShibbolethAuthorization.class);
				break;
			default:
				throw new IllegalArgumentException(
						"Unknown authorization type " + snaaServiceConfig.getShibbolethAuthorizationType()
				);
		}

		install(new ShibbolethAuthenticatorModule());

		bind(ShibbolethSNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(ShibbolethSNAA.class);
		bind(SNAAService.class).to(ShibbolethSNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}

	@Provides
	@Singleton
	ServedNodeUrnPrefixesProvider provideServedNodeUrnPrefixesProvider(final CommonConfig commonConfig) {
		return new ServedNodeUrnPrefixesProvider() {
			@Override
			public Set<NodeUrnPrefix> get() {
				return newHashSet(commonConfig.getUrnPrefix());
			}
		};
	}

	private AttributeBasedShibbolethAuthorizationAttributes parseAttributesFromProperties(final SNAAServiceConfig snaaServiceConfig) {

		final AttributeBasedShibbolethAuthorizationAttributes attributes =
				new AttributeBasedShibbolethAuthorizationAttributes();

		attributes.put(
				snaaServiceConfig.getShibbolethAuthorizationAttributeBasedKey(),
				snaaServiceConfig.getShibbolethAuthorizationAttributeBasedValue()
		);

		return attributes;
	}
}
