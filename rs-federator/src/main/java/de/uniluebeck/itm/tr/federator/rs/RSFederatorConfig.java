package de.uniluebeck.itm.tr.federator.rs;

import de.uniluebeck.itm.tr.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.common.config.UriToNodeUrnPrefixSetMapOptionHandler;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.Option;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public class RSFederatorConfig extends ConfigWithLogging {

	@Option(name = "--federates",
			usage = "(endpoint URL / URN prefix set)-pairs indicating which RS instances to federate (example: http://wisebed.itm.uni-luebeck.de/api/soap/v3.0/rs=urn:wisebed:uzl1:,urn:wisebed:uzl2:)",
			handler = UriToNodeUrnPrefixSetMapOptionHandler.class,
			required = true)
	public Map<URI, Set<NodeUrnPrefix>> federates;

	@Option(name = "--port",
			usage = "Port on which to run the RS federator (default: 8882)")
	public int port = 8882;

	@Option(name = "--contextPath",
			usage = "Context path on which to run the RS federator (default: \"/federator/soap/v3.0/rs\")")
	public String contextPath = "/federator/soap/v3.0/rs";
}
