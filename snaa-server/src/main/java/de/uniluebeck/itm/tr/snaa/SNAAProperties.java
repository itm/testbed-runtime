package de.uniluebeck.itm.tr.snaa;

public interface SNAAProperties {

	String CONTEXT_PATH = "snaa.context.path";

	String JAAS_LOGINMODULE = "snaa.jaas.loginmodule";

	String JAAS_CONFIG = "snaa.jaas.config";

	String SHIBBOLETH_IDPDOMAIN = "snaa.shibboleth.idpdomain";

	String SHIBBOLETH_URL = "snaa.shibboleth.url";

	String SHIBBOLETH_PROXY = "snaa.shibboleth.proxy";

	String SHIBBOLETH_AUTHORIZATION_TYPE = "snaa.shibboleth.authorization.type";

	String SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_USERNAME = "snaa.shibboleth.authorization.datasource.username";

	String SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_PASSWORD = "snaa.shibboleth.authorization.datasource.password";

	String SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_DATASOURCE_URL = "snaa.shibboleth.authorization.datasource.url";

	String SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_KEY_PREFIX = "snaa.shibboleth.authorization.key.";

	String SHIBBOLETH_AUTHORIZATION_ATTRIBUTEBASED_VALUE_PREFIX = "snaa.shibboleth.authorization.value.";

	String SHIRO_HIBERNATE_PROPERTIES = "snaa.shiro.hibernate.properties";
}
