package de.uniluebeck.itm.tr.iwsn.common.config;

import org.apache.log4j.Level;
import org.kohsuke.args4j.Option;

public class ConfigWithLogging extends Config {

	@Option(name = "--logLevel",
			usage = "Logging level (valid values: TRACE, DEBUG, INFO, WARN, ERROR).",
			handler = Log4JLevelOptionHandler.class
	)
	public Level logLevel = null;

	@Option(
			name = "--verbose",
			usage = "Verbose (DEBUG) logging output (default: INFO)."
	)
	public boolean verbose = false;

}
