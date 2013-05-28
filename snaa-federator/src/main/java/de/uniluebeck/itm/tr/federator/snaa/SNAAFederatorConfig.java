package de.uniluebeck.itm.tr.federator.snaa;

import de.uniluebeck.itm.tr.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.common.config.UriToNodeUrnPrefixSetMapOptionHandler;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.Option;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public class SNAAFederatorConfig extends ConfigWithLogging {

	@Option(name = "--federates",
			usage = "(endpoint URL / URN prefix set)-pairs indicating which SNAA instances to federate (example: http://wisebed.itm.uni-luebeck.de/api/soap/v3.0/snaa=urn:wisebed:uzl1:,urn:wisebed:uzl2:)",
			handler = UriToNodeUrnPrefixSetMapOptionHandler.class,
			required = true)
	public Map<URI, Set<NodeUrnPrefix>> federates;

	@Option(name = "--port",
			usage = "Port on which to run the SNAA federator (default: 8883)")
	public int port = 8883;

	@Option(name = "--contextPath",
			usage = "Context path on which to run the SNAA federator (default: \"/federator/soap/v3.0/snaa\")")
	public String contextPath = "/federator/soap/v3.0/snaa";
}
