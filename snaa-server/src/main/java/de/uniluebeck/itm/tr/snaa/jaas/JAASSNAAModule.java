package de.uniluebeck.itm.tr.snaa.jaas;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.snaa.SNAA;
import de.uniluebeck.itm.tr.snaa.AttributeBasedAuthorization;
import de.uniluebeck.itm.tr.snaa.IUserAuthorization;
import eu.wisebed.testbed.api.snaa.authorization.datasource.AuthorizationDataSource;
import eu.wisebed.testbed.api.snaa.authorization.datasource.ShibbolethDataSource;

import java.util.*;

import static com.google.common.base.Throwables.propagate;

public class JAASSNAAModule extends PrivateModule {

	private static final String AUTHORIZATION_ATT = ".authorization";

	private static final String AUTHORIZATION_KEY_ATT = ".key";

	private static final String AUTHORIZATION_VAL_ATT = ".value";

	private static final String AUTHORIZATION_DATA_SOURCE = ".datasource";

	private static final String AUTHORIZATION_DATA_SOURCE_USERNAME = ".username";

	private static final String AUTHORIZATION_DATA_SOURCE_PASSWORD = ".password";

	private static final String AUTHORIZATION_DATA_SOURCE_URL = ".url";

	private final SNAAConfig config;

	public JAASSNAAModule(final SNAAConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {

		requireBinding(ServicePublisher.class);

		bind(SNAAConfig.class).toInstance(config);

		bind(JAASSNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(JAASSNAA.class);
		bind(SNAAService.class).to(JAASSNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}

	@Provides
	@Singleton
	IUserAuthorization provideUserAuthorization() {

		try {
			final String authorizationClassName = config.getSnaaAuthorizationConfig().getProperty("jaas.authorization_class");
			final IUserAuthorization authorization = (IUserAuthorization) Class.forName(authorizationClassName).newInstance();

			if (AttributeBasedAuthorization.class.getCanonicalName().equals(authorizationClassName)) {
				Map<String, String> attributes = createAuthorizationAttributeMap();
				((AttributeBasedAuthorization) authorization).setAttributes(attributes);
				((AttributeBasedAuthorization) authorization).setDataSource(getAuthorizationDataSource());
			}

			return authorization;

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	private AuthorizationDataSource getAuthorizationDataSource()
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {

		final Properties authorizationConfig = config.getSnaaAuthorizationConfig();

		for (String key : authorizationConfig.stringPropertyNames()) {

			String dataSourceName = AUTHORIZATION_ATT + AUTHORIZATION_DATA_SOURCE;

			if (key.equals(dataSourceName)) {

				final Class<?> clazz = Class.forName(authorizationConfig.getProperty(key));
				AuthorizationDataSource dataSource = (AuthorizationDataSource) clazz.newInstance();

				String dataSourceUsername = authorizationConfig.getProperty(dataSourceName + AUTHORIZATION_DATA_SOURCE_USERNAME);
				String dataSourcePassword = authorizationConfig.getProperty(dataSourceName + AUTHORIZATION_DATA_SOURCE_PASSWORD);
				String dataSourceUrl = authorizationConfig.getProperty(dataSourceName + AUTHORIZATION_DATA_SOURCE_URL);

				if (dataSourceUsername != null) {
					dataSource.setUsername(dataSourceUsername);
				}

				if (dataSourcePassword != null) {
					dataSource.setPassword(dataSourcePassword);
				}

				if (dataSourceUrl != null) {
					dataSource.setUrl(dataSourceUrl);
				}

				return dataSource;
			}
		}

		//set default
		return new ShibbolethDataSource();
	}

	private Map<String, String> createAuthorizationAttributeMap() {

		final Map<String, String> attributes = new HashMap<String, String>();
		final List<String> keys = new LinkedList<String>();

		//getting keys from properties
		for (String key : config.getSnaaAuthorizationConfig().stringPropertyNames()) {
			if (key.startsWith(AUTHORIZATION_ATT) && key.endsWith(AUTHORIZATION_KEY_ATT)) {
				keys.add(key);
			}
		}

		for (String k : keys) {

			String key = config.getSnaaAuthorizationConfig().getProperty(k);

			//getting plain key-number from properties
			String plainKeyProperty = k.replaceAll(AUTHORIZATION_ATT + ".", "").replaceAll(AUTHORIZATION_KEY_ATT, "");
			String keyPrefix = AUTHORIZATION_ATT + "." + plainKeyProperty;

			//building value-property-string
			String value = config.getSnaaAuthorizationConfig().getProperty(keyPrefix + AUTHORIZATION_VAL_ATT);

			//finally put key and values
			attributes.put(key, value);
		}

		return attributes;
	}
}
