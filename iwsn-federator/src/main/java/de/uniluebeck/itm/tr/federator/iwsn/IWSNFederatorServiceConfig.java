package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.federator.utils.URIToNodeUrnPrefixSetMap;
import de.uniluebeck.itm.tr.federator.utils.UriToNodeUrnPrefixSetMapTypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

public class IWSNFederatorServiceConfig {

	@PropConf(
			usage = "(endpoint URL / URN prefix set)-pairs indicating which iWSN instances to federate",
			example = "http://portal.tb1and2.tld/api/soap/v3/sm=urn:wisebed:tb1:,urn:wisebed:tb2: http://portal.tb3.tld/api/soap/v3/sm=urn:wisebed:tb3:",
			typeConverter = UriToNodeUrnPrefixSetMapTypeConverter.class
	)
	public static final String FEDERATOR_FEDERATES = "federator.iwsn.federates";

	@Inject
	@Named(FEDERATOR_FEDERATES)
	private URIToNodeUrnPrefixSetMap federates;

	public URIToNodeUrnPrefixSetMap getFederates() {
		return federates;
	}
}
