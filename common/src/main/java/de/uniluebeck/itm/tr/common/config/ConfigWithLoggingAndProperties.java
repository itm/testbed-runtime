package de.uniluebeck.itm.tr.common.config;

import org.kohsuke.args4j.Option;

import java.util.Properties;

public class ConfigWithLoggingAndProperties extends ConfigWithLogging {

	@Option(name = "--config",
			usage = "The configuration .properties file",
			handler = PropertiesOptionHandler.class
	)
	public Properties config = null;
}
