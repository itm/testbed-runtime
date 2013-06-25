package de.uniluebeck.itm.tr.federator.rs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.config.UriToNodeUrnPrefixSetMapTypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public class RSFederatorConfig {

	@PropConf(
			usage = "(endpoint URL / URN prefix set)-pairs indicating which RS instances to federate",
			example = "http://wisebed.itm.uni-luebeck.de/api/soap/v3.0/rs=urn:wisebed:uzl1:,urn:wisebed:uzl2:",
			typeConverter = UriToNodeUrnPrefixSetMapTypeConverter.class
	)
	public static final String FEDERATOR_FEDERATES = "rs.federator.federates";

	@Inject
	@Named(FEDERATOR_FEDERATES)
	private Map<URI, Set<NodeUrnPrefix>> federates;

	@PropConf(
			usage = "Port on which to run the RS federator",
			example = "8882",
			defaultValue = "8882"
	)
	public static final String FEDERATOR_PORT = "rs.federator.port";

	@Inject
	@Named(FEDERATOR_PORT)
	private int port;

	@PropConf(
			usage = "Context path on which to run the RS federator",
			example = "/federator/soap/v3.0/rs",
			defaultValue = "/federator/soap/v3.0/rs"
	)
	public static final String FEDERATOR_CONTEXT_PATH = "rs.federator.context_path";

	@Inject
	@Named(FEDERATOR_CONTEXT_PATH)
	private String contextPath;

	public String getContextPath() {
		return contextPath;
	}

	public Map<URI, Set<NodeUrnPrefix>> getFederates() {
		return federates;
	}

	public int getPort() {
		return port;
	}
}
