package de.uniluebeck.itm.tr.snaa;

import de.uniluebeck.itm.tr.common.config.CommonConfigImpl;
import de.uniluebeck.itm.tr.common.config.PropertiesOptionHandler;
import org.kohsuke.args4j.Option;

import java.util.Properties;

public class SNAAConfigImpl extends CommonConfigImpl implements SNAAConfig {

	@Option(name = "--snaaContextPath",
			usage = "Context path on which to run the SNAA service (default: \"/soap/v3/snaa\")")
	protected String snaaContextPath = "/soap/v3/snaa";

	@Option(name = "--snaaType",
			usage = "The authentication backend of the SNAA service",
			required = true)
	protected SNAAType snaaType;

	@Option(name = "--snaaProperties",
			usage = "The properties file containing the configuration for the SNAA",
			handler = PropertiesOptionHandler.class)
	protected Properties snaaProperties;

	@Override
	public String getSnaaContextPath() {
		return snaaContextPath;
	}

	@Override
	public SNAAType getSnaaType() {
		return snaaType;
	}

	@Override
	public Properties getSnaaProperties() {
		return snaaProperties;
	}

	@SuppressWarnings("unused")
	public void setSnaaProperties(final Properties snaaProperties) {
		this.snaaProperties = snaaProperties;
	}

	@SuppressWarnings("unused")
	public void setSnaaType(final SNAAType snaaType) {
		this.snaaType = snaaType;
	}

	@SuppressWarnings("unused")
	public void setSnaaContextPath(final String snaaContextPath) {
		this.snaaContextPath = snaaContextPath;
	}
}
