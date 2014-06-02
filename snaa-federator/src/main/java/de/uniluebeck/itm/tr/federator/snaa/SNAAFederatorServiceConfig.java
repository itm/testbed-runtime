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

	public URIToNodeUrnPrefixSetMap getFederates() {
		return federates;
	}
}
