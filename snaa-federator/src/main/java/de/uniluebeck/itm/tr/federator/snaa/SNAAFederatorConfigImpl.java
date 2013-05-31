package de.uniluebeck.itm.tr.federator.snaa;

import de.uniluebeck.itm.tr.common.config.CommonConfigImpl;
import de.uniluebeck.itm.tr.common.config.PropertiesOptionHandler;
import de.uniluebeck.itm.tr.common.config.UriToNodeUrnPrefixSetMapOptionHandler;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.Option;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class SNAAFederatorConfigImpl extends CommonConfigImpl implements SNAAFederatorConfig {

	@Option(name = "--federates",
			usage = "(endpoint URL / URN prefix set)-pairs indicating which SNAA instances to federate (example: http://wisebed.itm.uni-luebeck.de/api/soap/v3.0/snaa=urn:wisebed:uzl1:,urn:wisebed:uzl2:)",
			handler = UriToNodeUrnPrefixSetMapOptionHandler.class,
			required = true)
	public Map<URI, Set<NodeUrnPrefix>> federates;

	@Option(name = "--snaaContextPath",
			usage = "Context path on which to run the SNAA federator (default: \"/federator/soap/v3.0/snaa\")")
	public String snaaContextPath = "/federator/soap/v3.0/snaa";

	@Option(name = "--snaaFederatorType",
			usage = "Type of federator to run (API is purely API-based, SHIBBOLETH uses Shibboleth for authentication and API for authorization)")
	public SNAAFederatorType snaaFederatorType;

	@Option(name = "--snaaFederatorProperties",
			usage = "The properties file containing the configuration for the SNAA federator",
			handler = PropertiesOptionHandler.class)
	public Properties snaaFederatorProperties;

	@Override
	public Map<URI, Set<NodeUrnPrefix>> getFederates() {
		return federates;
	}

	@Override
	public String getSnaaContextPath() {
		return snaaContextPath;
	}

	@Override
	public SNAAFederatorType getSnaaFederatorType() {
		return snaaFederatorType;
	}

	@Override
	public Properties getSnaaFederatorProperties() {
		return snaaFederatorProperties;
	}

	@SuppressWarnings("unused")
	public void setFederates(final Map<URI, Set<NodeUrnPrefix>> federates) {
		this.federates = federates;
	}

	@SuppressWarnings("unused")
	public void setSnaaContextPath(final String snaaContextPath) {
		this.snaaContextPath = snaaContextPath;
	}

	@SuppressWarnings("unused")
	public void setSnaaFederatorType(final SNAAFederatorType snaaFederatorType) {
		this.snaaFederatorType = snaaFederatorType;
	}

	@SuppressWarnings("unused")
	public void setSnaaFederatorProperties(final Properties snaaFederatorProperties) {
		this.snaaFederatorProperties = snaaFederatorProperties;
	}
}
