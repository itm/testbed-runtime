package de.uniluebeck.itm.tr.rs;

import de.uniluebeck.itm.tr.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.common.config.NodeUrnPrefixOptionHandler;
import de.uniluebeck.itm.tr.common.config.PropertiesOptionHandler;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.Option;

import java.net.URI;
import java.util.Properties;
import java.util.TimeZone;

public class RSConfig extends ConfigWithLogging {

	public static enum Persistence {
		JPA, GCAL, IN_MEMORY;
	}

	@Option(name = "--urnPrefix",
			usage = "URN prefix for which this RS is responsible",
			handler = NodeUrnPrefixOptionHandler.class,
			required = true)
	public NodeUrnPrefix urnPrefix;

	@Option(name = "--snaaEndpointUrl",
			usage = "Endpoint URL of the SNAA service",
			required = true)
	public URI snaaEndpointUrl;

	@Option(name = "--smEndpointUrl",
			usage = "Endpoint URL of the session management (SM) service",
			required = true)
	public URI smEndpointUrl;

	@Option(name = "--persistence",
			usage = "Persistence layer to use",
			required = true)
	public Persistence persistence;

	@Option(name = "--persistenceConfig",
			usage = "Persistence layer configuration file",
			handler = PropertiesOptionHandler.class)
	public Properties persistenceConfig;

	@Option(name = "--port",
			usage = "Port on which to run the RS (default: 8889)")
	public int port = 8889;

	@Option(name = "--contextPath",
			usage = "Context path on which to run the RS federator (default: \"/soap/v3.0/rs\")")
	public String contextPath = "/soap/v3.0/rs";

	@Option(name = "--timezone",
			usage = "Time zone of the RS (default: GMT)",
			handler = TimeZoneOptionHandler.class)
	public TimeZone timeZone = TimeZone.getTimeZone("GMT");
}
