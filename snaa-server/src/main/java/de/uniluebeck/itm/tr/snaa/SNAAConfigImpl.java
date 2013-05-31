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

	@Option(name = "--snaaProperties",
			usage = "The properties file containing the configuration for the SNAA",
			handler = PropertiesOptionHandler.class)
	protected Properties snaaProperties;

	@Override
	public String getSnaaContextPath() {
		return snaaContextPath;
	}

	@Override
	public SNAAAuthenticationType getSnaaAuthenticationType() {
		return snaaAuthenticationType;
	}

	@Override
	public Properties getSnaaProperties() {
		return snaaProperties;
	}

	@Override
	public SNAAAuthorizationType getSnaaAuthorizationType() {
		return snaaAuthorizationType;
	}

	@SuppressWarnings("unused")
	public void setSnaaProperties(final Properties snaaProperties) {
		this.snaaProperties = snaaProperties;
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
