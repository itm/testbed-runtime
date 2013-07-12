package de.uniluebeck.itm.tr.federator.snaa;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.converters.PropertiesTypeConverter;
import de.uniluebeck.itm.tr.federator.utils.URIToNodeUrnPrefixSetMap;
import de.uniluebeck.itm.tr.federator.utils.UriToNodeUrnPrefixSetMapTypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import java.util.Properties;

public class SNAAFederatorServiceConfig {

	@PropConf(
			usage = "(endpoint URL / URN prefix set)-pairs indicating which SNAA instances to federate",
			example = "http://portal.tb1and2.tld/api/soap/v3.0/snaa=urn:wisebed:tb1:,urn:wisebed:tb2: http://portal.tb3.tld/api/soap/v3.0/snaa=urn:wisebed:tb3:",
			typeConverter = UriToNodeUrnPrefixSetMapTypeConverter.class
	)
	public static final String FEDERATOR_FEDERATES = "federator.snaa.federates";

	@Inject
	@Named(FEDERATOR_FEDERATES)
	private URIToNodeUrnPrefixSetMap federates;

	@PropConf(
			usage = "Context path on which to run the SNAA federator",
			example = "/federator/soap/v3.0/snaa",
			defaultValue = "/federator/soap/v3.0/snaa"
	)
	public static final String FEDERATOR_CONTEXT_PATH = "federator.snaa.context_path";

	@Inject
	@Named(FEDERATOR_CONTEXT_PATH)
	private String snaaContextPath;

	@PropConf(
			usage = "Type of federator to run (API is purely API-based, SHIBBOLETH uses Shibboleth for authentication and API for authorization)",
			example = "API/SHIBBOLETH",
			defaultValue = "API"
	)
	public static final String FEDERATOR_TYPE = "federator.snaa.type";

	@Inject
	@Named(FEDERATOR_TYPE)
	private SNAAFederatorType snaaFederatorType;

	@PropConf(
			usage = "The properties file containing the configuration for the SNAA federator (only if SHIBBOLETH is used)",
			typeConverter = PropertiesTypeConverter.class
	)
	public static final String FEDERATOR_SHIBBOLETH_PROPERTIES = "federator.snaa.shibboleth.properties";

	@Inject
	@Named(FEDERATOR_SHIBBOLETH_PROPERTIES)
	private Properties snaaFederatorProperties;

	public String getSnaaContextPath() {
		return snaaContextPath;
	}

	public Properties getSnaaFederatorProperties() {
		return snaaFederatorProperties;
	}

	public SNAAFederatorType getSnaaFederatorType() {
		return snaaFederatorType;
	}

	public URIToNodeUrnPrefixSetMap getFederates() {
		return federates;
	}
}
