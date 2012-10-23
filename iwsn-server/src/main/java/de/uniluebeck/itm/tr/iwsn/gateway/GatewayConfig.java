package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.net.HostAndPort;
import de.uniluebeck.itm.tr.iwsn.util.Log4JLevelOptionHandler;
import org.apache.log4j.Level;
import org.kohsuke.args4j.Option;

public class GatewayConfig {

	@Option(name = "--portalAddress", usage = "Hostname and port of the portal server (e.g. portal:1234)", required = true)
	public HostAndPort portalAddress;

	@Option(name = "--logLevel",
			usage = "Set logging level (valid values: TRACE, DEBUG, INFO, WARN, ERROR).",
			handler = Log4JLevelOptionHandler.class)
	public Level logLevel = null;

	@Option(name = "--verbose", usage = "Verbose (DEBUG) logging output (default: INFO).")
	public boolean verbose = false;

	@Option(name = "--help", usage = "This help message.")
	public boolean help = false;

}
