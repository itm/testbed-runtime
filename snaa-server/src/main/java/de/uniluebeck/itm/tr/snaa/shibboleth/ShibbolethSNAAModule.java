package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.inject.util.Providers.of;

public class ShibbolethSNAAModule extends PrivateModule {

	private final CommonConfig commonConfig;

	private final SNAAConfig snaaConfig;

	public ShibbolethSNAAModule(final CommonConfig commonConfig, final SNAAConfig snaaConfig) {
		this.commonConfig = commonConfig;
		this.snaaConfig = snaaConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(of(commonConfig));
		bind(SNAAConfig.class).toProvider(of(snaaConfig));

		switch (snaaConfig.getShibbolethAuthorizationType()) {
			case ALWAYS_ALLOW:
				bind(ShibbolethAuthorization.class).to(AlwaysAllowShibbolethAuthorization.class).in(Scopes.SINGLETON);
				break;
			case ALWAYS_DENY:
				bind(ShibbolethAuthorization.class).to(AlwaysDenyShibbolethAuthorization.class).in(Scopes.SINGLETON);
				break;
			case ATTRIBUTE_BASED:
				bind(AttributeBasedShibbolethAuthorizationAttributes.class)
						.toInstance(parseAttributesFromProperties(snaaConfig));
				bind(ShibbolethAuthorization.class).to(AttributeBasedShibbolethAuthorization.class);
				break;
			default:
				throw new IllegalArgumentException(
						"Unknown authorization type " + snaaConfig.getShibbolethAuthorizationType()
				);
		}

		install(new ShibbolethAuthenticatorModule(snaaConfig));

		bind(SNAAService.class).to(ShibbolethSNAA.class).in(Scopes.SINGLETON);

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

	private AttributeBasedShibbolethAuthorizationAttributes parseAttributesFromProperties(final SNAAConfig snaaConfig) {

		final AttributeBasedShibbolethAuthorizationAttributes attributes =
				new AttributeBasedShibbolethAuthorizationAttributes();

		attributes.put(
				snaaConfig.getShibbolethAuthorizationAttributeBasedKey(),
				snaaConfig.getShibbolethAuthorizationAttributeBasedValue()
		);

		return attributes;
	}
}
