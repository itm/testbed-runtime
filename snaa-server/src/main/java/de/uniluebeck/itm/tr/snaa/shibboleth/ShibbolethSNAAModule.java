package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAProperties;
import de.uniluebeck.itm.tr.snaa.SNAAService;

import java.util.Properties;

import static de.uniluebeck.itm.tr.snaa.SNAAProperties.*;

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

		final ShibbolethAuthorizationType authorizationType = ShibbolethAuthorizationType
				.valueOf(snaaProperties.getProperty(SNAAProperties.SHIBBOLETH_AUTHORIZATION_TYPE));

		switch (authorizationType) {
			case ALWAYS_ALLOW:
				bind(ShibbolethAuthorization.class).to(AlwaysAllowShibbolethAuthorization.class).in(Scopes.SINGLETON);
				break;
			case ALWAYS_DENY:
				bind(ShibbolethAuthorization.class).to(AlwaysDenyShibbolethAuthorization.class).in(Scopes.SINGLETON);
				break;
			case ATTRIBUTE_BASED:
				bind(String.class)
						.annotatedWith(Names.named(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_URL))
						.toInstance((String) snaaProperties.get(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_URL)
						);
				bind(String.class)
						.annotatedWith(Names.named(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_USERNAME))
						.toInstance(
								(String) snaaProperties.get(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_USERNAME)
						);
				bind(String.class)
						.annotatedWith(Names.named(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_PASSWORD))
						.toInstance(
								(String) snaaProperties.get(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_PASSWORD)
						);
				bind(AttributeBasedShibbolethAuthorizationAttributes.class).toInstance(parseAttributesFromProperties());
				bind(ShibbolethAuthorization.class).to(AttributeBasedShibbolethAuthorization.class);
				break;
			default:
				throw new IllegalArgumentException("Unknown authorization type " + authorizationType);
		}

		final ShibbolethAuthenticatorConfig authenticatorConfig =
				new ShibbolethAuthenticatorConfig(snaaProperties);
		install(new ShibbolethAuthenticatorModule(authenticatorConfig));

		bind(String.class).annotatedWith(Names.named("snaaContextPath")).toInstance(snaaContextPath);
		bind(SNAAService.class).to(ShibbolethSNAA.class).in(Scopes.SINGLETON);
	}

	private AttributeBasedShibbolethAuthorizationAttributes parseAttributesFromProperties() {

		final AttributeBasedShibbolethAuthorizationAttributes attributes =
				new AttributeBasedShibbolethAuthorizationAttributes();

		for (String key : snaaProperties.stringPropertyNames()) {
			if (key.startsWith(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_KEY_PREFIX)) {

				// read out number that follows key prefix (e.g. snaa.shibboleth.authorization.key.1)
				String attributeKey = snaaProperties.getProperty(key);
				String attributeKeyNumber =
						key.substring(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_KEY_PREFIX.length());
				String attributeValue = snaaProperties
						.getProperty(SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_KEY_PREFIX + attributeKeyNumber);

				attributes.put(attributeKey, attributeValue);
			}
		}

		return attributes;
	}
}
