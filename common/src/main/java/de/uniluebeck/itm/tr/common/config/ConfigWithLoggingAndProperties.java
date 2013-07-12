package de.uniluebeck.itm.tr.common.config;

import de.uniluebeck.itm.util.args4j.PropertiesOptionHandler;
import org.kohsuke.args4j.Option;

import java.util.Properties;

public class ConfigWithLoggingAndProperties extends ConfigWithLogging {

	@Option(name = "--config",
			usage = "The configuration .properties file",
			handler = PropertiesOptionHandler.class,
			required = true
	)
	public Properties config = null;

	@Option(name = "--helpConfig",
			usage = "Prints available configuration properties including documentation")
	public boolean helpConfig;
}
