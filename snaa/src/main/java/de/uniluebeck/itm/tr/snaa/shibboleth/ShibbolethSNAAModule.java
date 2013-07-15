package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import eu.wisebed.api.v3.snaa.SNAA;

public class ShibbolethSNAAModule extends PrivateModule {

	private final SNAAServiceConfig snaaServiceConfig;

	public ShibbolethSNAAModule(final SNAAServiceConfig snaaServiceConfig) {
		this.snaaServiceConfig = snaaServiceConfig;
	}

	@Override
	protected void configure() {

		requireBinding(CommonConfig.class);
		requireBinding(SNAAServiceConfig.class);
		requireBinding(ServedNodeUrnPrefixesProvider.class);

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
