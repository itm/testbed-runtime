package de.uniluebeck.itm.tr.snaa;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import de.uniluebeck.itm.util.propconf.converters.PropertiesTypeConverter;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;
import org.apache.shiro.crypto.hash.Sha512Hash;

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
			example = "DUMMY/JAAS/SHIRO/REMOTE"
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
