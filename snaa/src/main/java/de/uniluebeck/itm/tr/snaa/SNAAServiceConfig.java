package de.uniluebeck.itm.tr.snaa;

import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.converters.HostAndPortTypeConverter;
import de.uniluebeck.itm.util.propconf.converters.PropertiesTypeConverter;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethAuthorizationType;
import de.uniluebeck.itm.util.propconf.PropConf;
import org.apache.shiro.crypto.hash.Sha512Hash;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Properties;

public class SNAAServiceConfig {

	@PropConf(
			usage = "Context path on which to run the SNAA service",
			example = "/soap/v3/snaa",
			defaultValue = "/soap/v3/snaa"
	)
	public static final String SNAA_CONTEXT_PATH = "snaa.context_path";

	@Inject
	@Named(SNAA_CONTEXT_PATH)
	private String snaaContextPath;

	@PropConf(
			usage = "The authentication backend of the SNAA service",
			example = "DUMMY/JAAS/SHIBBOLETH/SHIRO/REMOTE"
	)
	public static final String SNAA_TYPE = "snaa.type";

	@Inject
	@Named(SNAA_TYPE)
	private SNAAType snaaType;

	@PropConf(
			usage = "The URI of the remote SNAA server (only if REMOTE is used)",
			example = "http://localhost:8890/soap/v3/snaa",
			typeConverter = URITypeConverter.class
	)
	public static final String SNAA_REMOTE_URI = "snaa.remote.uri";

	@Inject(optional = true)
	@Named(SNAA_REMOTE_URI)
	private URI snaaRemoteUri;

	@PropConf(
			usage = "The login module for the JAAS backend (only if JAAS is used)",
			example = "edu.internet2.middleware.shibboleth.jaas.htpasswd.HtpasswdLoginModule"
	)
	public static final String JAAS_LOGINMODULE = "snaa.jaas.loginmodule";

	@Inject(optional = true)
	@Named(JAAS_LOGINMODULE)
	private String jaasLoginModule;

	@PropConf(
			usage = "The JAAS backends configuration file (only if JAAS is used)"
	)
	public static final String JAAS_CONFIG_FILE = "snaa.jaas.config_file";

	@Inject(optional = true)
	@Named(JAAS_CONFIG_FILE)
	private String jaasConfigFile;

	@PropConf(
			usage = "The URL of the Shibboleth authentication service (only if SHIBBOLETH is used)",
			example = "https://wisebed2.itm.uni-luebeck.de/portal/TARWIS/Welcome/welcomeIndex.php"
	)
	public static final String SHIBBOLETH_URL = "snaa.shibboleth.url";

	@Inject(optional = true)
	@Named(SHIBBOLETH_URL)
	private URI shibbolethUrl;

	@PropConf(
			usage = "A proxy server to be used for the Shibboleth authentication service (only if SHIBBOLETH is used)",
			typeConverter = HostAndPortTypeConverter.class
	)
	public static final String SHIBBOLETH_PROXY = "snaa.shibboleth.proxy";

	@Inject(optional = true)
	@Named(SHIBBOLETH_PROXY)
	private HostAndPort shibbolethProxy;

	@PropConf(
			usage = "The type of authorization backend to be used together with Shibboleth authentication (only if SHIBBOLETH is used)",
			example = "ALWAYS_ALLOW/ALWAYS_DENY/ATTRIBUTE_BASED",
			defaultValue = "ALWAYS_ALLOW"
	)
	public static final String SHIBBOLETH_AUTHORIZATION_TYPE = "snaa.shibboleth.authorization.type";

	@Inject(optional = true)
	@Named(SHIBBOLETH_AUTHORIZATION_TYPE)
	private ShibbolethAuthorizationType shibbolethAuthorizationType;

	@PropConf(
			usage = "The username for the attribute based authorization backend data source used together with Shibboleth authentication (only if SHIBBOLETH and ATTRIBUTE_BASED are used)"
	)
	public static final String SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_DATASOURCE_USERNAME =
			"snaa.shibboleth.authorization.attribute_based.username";

	@Inject(optional = true)
	@Named(SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_DATASOURCE_USERNAME)
	private String shibbolethAuthorizationAttributeBasedDatasourceUsername;

	@PropConf(
			usage = "The password for the attribute based authorization backend data source used together with Shibboleth authentication (only if SHIBBOLETH and ATTRIBUTE_BASED are used)"
	)
	public static final String SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_DATASOURCE_PASSWORD =
			"snaa.shibboleth.authorization.attribute_based.password";

	@Inject(optional=true)
	@Named(SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_DATASOURCE_PASSWORD)
	private String shibbolethAuthorizationAttributeBasedDatasourcePassword;

	@PropConf(
			usage = "The URL for the attribute based authorization backend data source used together with Shibboleth authentication (only if SHIBBOLETH and ATTRIBUTE_BASED are used)",
			example = "jdbc:mysql://localhost:3306/snaportal",
			typeConverter = URITypeConverter.class
	)
	public static final String SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_DATASOURCE_URL =
			"snaa.shibboleth.authorization.attribute_based.datasource.url";

	@Inject(optional = true)
	@Named(SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_DATASOURCE_URL)
	private URI shibbolethAuthorizationAttributeBasedDatasourceUrl;

	@PropConf(
			usage = "The key for the attribute based authorization backend data source used together with Shibboleth authentication (only if SHIBBOLETH and ATTRIBUTE_BASED are used)",
			example = "homeOrganization",
			defaultValue = "homeOrganization"
	)
	public static final String SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_KEY =
			"snaa.shibboleth.authorization.attribute_based.key";

	@Inject(optional=true)
	@Named(SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_KEY)
	private String shibbolethAuthorizationAttributeBasedKey;

	@PropConf(
			usage = "The value for the attribute based authorization backend data source used together with Shibboleth authentication (only if SHIBBOLETH and ATTRIBUTE_BASED are used)",
			example = "wisebed1.itm.uni-luebeck.de"
	)
	public static final String SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_VALUE =
			"snaa.shibboleth.authorization.attribute_based.value";

	@Inject(optional=true)
	@Named(SHIBBOLETH_AUTHORIZATION_ATTRIBUTE_BASED_VALUE)
	private String shibbolethAuthorizationAttributeBasedValue;

	@PropConf(
			usage = "The JPA (Hibernate) .properties file for the Shiro authentication and authorization backend (only if SHIRO is used)",
			typeConverter = PropertiesTypeConverter.class
	)
	public static final String SHIRO_JPA_PROPERTIES_FILE = "snaa.shiro.jpa.properties_file";

	@Inject(optional=true)
	@Named(SHIRO_JPA_PROPERTIES_FILE)
	private Properties shiroJpaProperties;

	@PropConf(
			usage = "The name of the hash algorithm to be used with Shiro authentication and authorization backend (only if SHIRO is used)",
			example = Sha512Hash.ALGORITHM_NAME,
			defaultValue = Sha512Hash.ALGORITHM_NAME
	)
	public static final String SHIRO_HASH_ALGORITHM_NAME = "snaa.shiro.hash_algorithm.name";

	@Inject(optional=true)
	@Named(SHIRO_HASH_ALGORITHM_NAME)
	private String shiroHashAlgorithmName;

	@PropConf(
			usage = "The number of iterations to be run for the hash algorithm to be used with Shiro authentication and authorization backend (only if SHIRO is used)",
			example = "1000",
			defaultValue = "1000"
	)
	public static final String SHIRO_HASH_ALGORITHM_ITERATIONS = "snaa.shiro.hash_algorithm.iterations";

	@Inject
	@Named(SHIRO_HASH_ALGORITHM_ITERATIONS)
	private int shiroHashAlgorithmIterations;

	@PropConf(
			usage = "The context path of the ShiroSNAA admin frontend REST API",
			example = "/rest/v1/shirosnaa/admin",
			defaultValue = "/rest/v1/shirosnaa/admin"
	)
	public static final String SHIRO_ADMIN_REST_API_CONTEXTPATH = "snaa.shiro.admin.rest_api.contextpath";

	@Inject
	@Named(SHIRO_ADMIN_REST_API_CONTEXTPATH)
	private String shiroAdminRestApiContextPath;

	@PropConf(
			usage = "The context path of the ShiroSNAA admin frontend webapp",
			example = "/shirosnaa",
			defaultValue = "/shirosnaa"
	)
	public static final String SHIRO_ADMIN_WEBAPP_CONTEXTPATH = "snaa.shiro.admin.webapp.contextpath";

	@Inject
	@Named(SHIRO_ADMIN_WEBAPP_CONTEXTPATH)
	private String shiroAdminWebappContextPath;

	@PropConf(
			usage = "The context path on which to run the device database REST API",
			example = "/rest/v1.0/devicedb",
			defaultValue = "/rest/v1.0/devicedb"
	)
	public static final String SHIRO_ADMIN_DEVICE_DB_REST_API_CONTEXTPATH = "devicedb.rest_api.context_path";

	@Inject
	@Named(SHIRO_ADMIN_DEVICE_DB_REST_API_CONTEXTPATH)
	private String shiroAdminDeviceDBRestApiContextPath;

	public String getShiroAdminWebappContextPath() {
		return shiroAdminWebappContextPath;
	}

	public String getShiroAdminDeviceDBRestApiContextPath() {
		return shiroAdminDeviceDBRestApiContextPath;
	}

	public String getShiroAdminRestApiContextPath() {
		return shiroAdminRestApiContextPath;
	}

	public String getShiroHashAlgorithmName() {
		return shiroHashAlgorithmName;
	}

	public int getShiroHashAlgorithmIterations() {
		return shiroHashAlgorithmIterations;
	}

	public Properties getShiroJpaProperties() {
		return shiroJpaProperties;
	}

	public String getJaasConfigFile() {
		return jaasConfigFile;
	}

	public String getJaasLoginModule() {
		return jaasLoginModule;
	}

	public String getShibbolethAuthorizationAttributeBasedDatasourcePassword() {
		return shibbolethAuthorizationAttributeBasedDatasourcePassword;
	}

	public URI getShibbolethAuthorizationAttributeBasedDatasourceUrl() {
		return shibbolethAuthorizationAttributeBasedDatasourceUrl;
	}

	public String getShibbolethAuthorizationAttributeBasedDatasourceUsername() {
		return shibbolethAuthorizationAttributeBasedDatasourceUsername;
	}

	public String getShibbolethAuthorizationAttributeBasedKey() {
		return shibbolethAuthorizationAttributeBasedKey;
	}

	public String getShibbolethAuthorizationAttributeBasedValue() {
		return shibbolethAuthorizationAttributeBasedValue;
	}

	public ShibbolethAuthorizationType getShibbolethAuthorizationType() {
		return shibbolethAuthorizationType;
	}

	@Nullable
	public HostAndPort getShibbolethProxy() {
		return shibbolethProxy;
	}

	public URI getShibbolethUrl() {
		return shibbolethUrl;
	}

	public String getSnaaContextPath() {
		return snaaContextPath;
	}

	public SNAAType getSnaaType() {
		return snaaType;
	}

	public URI getSnaaRemoteUri() {
		return snaaRemoteUri;
	}
}
