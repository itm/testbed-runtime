package de.uniluebeck.itm.tr.federator.snaa;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.federator.utils.URIToNodeUrnPrefixSetMap;
import de.uniluebeck.itm.tr.federator.utils.UriToNodeUrnPrefixSetMapTypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

public class SNAAFederatorServiceConfig {

	@PropConf(
			usage = "(endpoint URL / URN prefix set)-pairs indicating which SNAA instances to federate",
			example = "http://portal.tb1and2.tld/soap/v3/snaa=urn:wisebed:tb1:,urn:wisebed:tb2: http://portal.tb3.tld/soap/v3/snaa=urn:wisebed:tb3:",
			typeConverter = UriToNodeUrnPrefixSetMapTypeConverter.class
	)
	public static final String FEDERATOR_FEDERATES = "federator.snaa.federates";

	@Inject
	@Named(FEDERATOR_FEDERATES)
	private URIToNodeUrnPrefixSetMap federates;

	@PropConf(
			usage = "Context path on which to run the SNAA federator",
			example = "/soap/v3/snaa",
			defaultValue = "/soap/v3/snaa"
	)
	public static final String FEDERATOR_CONTEXT_PATH = "federator.snaa.context_path";

	@Inject
	@Named(FEDERATOR_CONTEXT_PATH)
	private String snaaContextPath;

	@PropConf(
			usage = "Type of federator to run (API is purely API-based)",
			example = "API",
			defaultValue = "API"
	)
	public static final String FEDERATOR_TYPE = "federator.snaa.type";

	@Inject
	@Named(FEDERATOR_TYPE)
	private SNAAFederatorType snaaFederatorType;

	public String getSnaaContextPath() {
		return snaaContextPath;
	}

	public SNAAFederatorType getSnaaFederatorType() {
		return snaaFederatorType;
	}

	public URIToNodeUrnPrefixSetMap getFederates() {
		return federates;
	}
}
