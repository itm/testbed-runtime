package de.uniluebeck.itm.tr.federator.rs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.federator.utils.URIToNodeUrnPrefixSetMap;
import de.uniluebeck.itm.tr.federator.utils.UriToNodeUrnPrefixSetMapTypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

public class RSFederatorServiceConfig {

	@PropConf(
			usage = "(endpoint URL / URN prefix set)-pairs indicating which RS instances to federate",
			example = "http://portal.tb1and2.tld/soap/v3/rs=urn:wisebed:tb1:,urn:wisebed:tb2: http://portal.tb3.tld/soap/v3/rs=urn:wisebed:tb3:",
			typeConverter = UriToNodeUrnPrefixSetMapTypeConverter.class
	)
	public static final String FEDERATOR_FEDERATES = "federator.rs.federates";

	@Inject
	@Named(FEDERATOR_FEDERATES)
	private URIToNodeUrnPrefixSetMap federates;

	@PropConf(
			usage = "Context path on which to run the RS federator",
			example = "/soap/v3/rs",
			defaultValue = "/soap/v3/rs"
	)
	public static final String FEDERATOR_CONTEXT_PATH = "federator.rs.context_path";

	@Inject
	@Named(FEDERATOR_CONTEXT_PATH)
	private String contextPath;

	public String getContextPath() {
		return contextPath;
	}

	public URIToNodeUrnPrefixSetMap getFederates() {
		return federates;
	}

}
