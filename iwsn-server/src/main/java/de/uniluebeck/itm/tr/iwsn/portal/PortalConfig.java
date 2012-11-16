package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.net.HostAndPort;
import de.uniluebeck.itm.tr.iwsn.util.Log4JLevelOptionHandler;
import org.apache.log4j.Level;
import org.kohsuke.args4j.Option;

public class PortalConfig extends Config {

	@Option(name = "--portalAddress",
			usage = "Address for the portal to listen for the internal network (default: localhost:8080)")
	public HostAndPort portalAddress = HostAndPort.fromParts("localhost", 8080);

	@Option(name = "--logLevel",
			usage = "Logging level (valid values: TRACE, DEBUG, INFO, WARN, ERROR).",
			handler = Log4JLevelOptionHandler.class)
	public Level logLevel = null;

	@Option(name = "--verbose", usage = "Verbose (DEBUG) logging output (default: INFO).")
	public boolean verbose = false;

}
