package de.uniluebeck.itm.tr.federator.snaa;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.config.PropertiesTypeConverter;
import de.uniluebeck.itm.tr.federatorutils.URIToNodeUrnPrefixSetMap;
import de.uniluebeck.itm.tr.federatorutils.UriToNodeUrnPrefixSetMapTypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import java.util.Properties;

public class SNAAFederatorConfig {

	@PropConf(
			usage = "(endpoint URL / URN prefix set)-pairs indicating which SNAA instances to federate",
			example = "http://wisebed.itm.uni-luebeck.de/api/soap/v3.0/snaa=urn:wisebed:uzl1:,urn:wisebed:uzl2:",
			typeConverter = UriToNodeUrnPrefixSetMapTypeConverter.class
	)
	public static final String FEDERATOR_FEDERATES = "snaa.federator.federates";

	@Inject
	@Named(FEDERATOR_FEDERATES)
	private URIToNodeUrnPrefixSetMap federates;

	@PropConf(
			usage = "Port on which to run the SNAA federator",
			example = "8883",
			defaultValue = "8883"
	)
	public static final String FEDERATOR_PORT = "snaa.federator.port";

	@Inject
	@Named(FEDERATOR_PORT)
	private int port;

	@PropConf(
			usage = "Context path on which to run the SNAA federator",
			example = "/federator/soap/v3.0/snaa",
			defaultValue = "/federator/soap/v3.0/snaa"
	)
	public static final String FEDERATOR_CONTEXT_PATH = "snaa.federator.context_path";

	@Inject
	@Named(FEDERATOR_CONTEXT_PATH)
	private String snaaContextPath;

	@PropConf(
			usage = "Type of federator to run (API is purely API-based, SHIBBOLETH uses Shibboleth for authentication and API for authorization)",
			example = "API/SHIBBOLETH",
			defaultValue = "API"
	)
	public static final String FEDERATOR_TYPE = "snaa.federator.type";

	@Inject
	@Named(FEDERATOR_TYPE)
	private SNAAFederatorType snaaFederatorType;

	@PropConf(
			usage = "The properties file containing the configuration for the SNAA federator",
			typeConverter = PropertiesTypeConverter.class
	)
	public static final String FEDERATOR_PROPERTIES = "snaa.federator.properties";

	@Inject
	@Named(FEDERATOR_PROPERTIES)
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

	public int getPort() {
		return port;
	}
}
