package de.uniluebeck.itm.tr.snaa;

import de.uniluebeck.itm.tr.common.config.CommonConfigImpl;
import de.uniluebeck.itm.tr.common.config.PropertiesOptionHandler;
import org.kohsuke.args4j.Option;

import java.util.Properties;

public class SNAAConfigImpl extends CommonConfigImpl implements SNAAConfig {

	@Option(name = "--snaaContextPath",
			usage = "Context path on which to run the SNAA service (default: \"/soap/v3/snaa\")")
	protected String snaaContextPath = "/soap/v3/snaa";

	@Option(name = "--snaaAuthenticationType",
			usage = "The authentication backend of the SNAA service",
			required = true)
	protected SNAAAuthenticationType snaaAuthenticationType;

	@Option(name = "--snaaAuthorizationType",
			usage = "The authorization backend of the SNAA service",
			required = true)
	protected SNAAAuthorizationType snaaAuthorizationType;

	@Option(name = "--snaaAuthorizationConfig",
			usage = "The properties file containing the configuration for the authorization backend",
			handler = PropertiesOptionHandler.class)
	protected Properties snaaAuthorizationConfig;

	@Option(name = "--snaaAuthenticationConfig",
			usage = "The properties file containing the configuration for the authentication backend",
			handler = PropertiesOptionHandler.class)
	protected Properties snaaAuthenticationConfig;

	@Override
	public String getSnaaContextPath() {
		return snaaContextPath;
	}

	@Override
	public SNAAAuthenticationType getSnaaAuthenticationType() {
		return snaaAuthenticationType;
	}

	@Override
	public Properties getSnaaAuthenticationConfig() {
		return snaaAuthenticationConfig;
	}

	@Override
	public Properties getSnaaAuthorizationConfig() {
		return snaaAuthorizationConfig;
	}

	@Override
	public SNAAAuthorizationType getSnaaAuthorizationType() {
		return snaaAuthorizationType;
	}

	@SuppressWarnings("unused")
	public void setSnaaAuthenticationConfig(final Properties snaaAuthenticationConfig) {
		this.snaaAuthenticationConfig = snaaAuthenticationConfig;
	}

	@SuppressWarnings("unused")
	public void setSnaaAuthorizationConfig(final Properties snaaAuthorizationConfig) {
		this.snaaAuthorizationConfig = snaaAuthorizationConfig;
	}

	@SuppressWarnings("unused")
	public void setSnaaAuthenticationType(final SNAAAuthenticationType snaaAuthenticationType) {
		this.snaaAuthenticationType = snaaAuthenticationType;
	}

	@SuppressWarnings("unused")
	public void setSnaaAuthorizationType(final SNAAAuthorizationType snaaAuthorizationType) {
		this.snaaAuthorizationType = snaaAuthorizationType;
	}

	@SuppressWarnings("unused")
	public void setSnaaContextPath(final String snaaContextPath) {
		this.snaaContextPath = snaaContextPath;
	}
}
