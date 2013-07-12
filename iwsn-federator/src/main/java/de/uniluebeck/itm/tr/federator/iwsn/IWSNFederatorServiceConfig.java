package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;
import de.uniluebeck.itm.tr.federator.utils.URIToNodeUrnPrefixSetMap;
import de.uniluebeck.itm.tr.federator.utils.UriToNodeUrnPrefixSetMapTypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import java.net.URI;

public class IWSNFederatorServiceConfig {

	@PropConf(
			usage = "(endpoint URL / URN prefix set)-pairs indicating which iWSN instances to federate",
			example = "http://portal.tb1and2.tld/api/soap/v3.0/sm=urn:wisebed:tb1:,urn:wisebed:tb2: http://portal.tb3.tld/api/soap/v3.0/sm=urn:wisebed:tb3:",
			typeConverter = UriToNodeUrnPrefixSetMapTypeConverter.class
	)
	public static final String FEDERATOR_FEDERATES = "federator.iwsn.federates";

	@Inject
	@Named(FEDERATOR_FEDERATES)
	private URIToNodeUrnPrefixSetMap federates;

	@PropConf(
			usage = "Context path base under which to run the federated WSN instances",
			example = "http://federator.mydomain.tld/federator/soap/v3.0/wsn"
	)
	public static final String FEDERATOR_WSN_ENDPOINT_URI_BASE = "federator.iwsn.wsn_endpoint_uri_base";

	@Inject
	@Named(FEDERATOR_WSN_ENDPOINT_URI_BASE)
	private URI wsnEndpointUriBase;

	@PropConf(
			usage = "Context path base under which to run the Controller instances that receive data from federated testbeds",
			example = "/federator/soap/v3.0/controller",
			defaultValue = "/federator/soap/v3.0/controller"
	)
	public static final String FEDERATOR_CONTROLLER_CONTEXT_PATH_BASE = "federator.iwsn.controller_context_path_base";

	@Inject
	@Named(FEDERATOR_CONTROLLER_CONTEXT_PATH_BASE)
	private String controllerContextPathBase;

	@PropConf(
			usage = "The Endpoint URI of the federator SessionManagement service (to be returned by SessionManagement.getConfiguration())",
			example = "http://federator.mydomain.tld/sm",
			typeConverter = URITypeConverter.class
	)
	public static final String FEDERATOR_SM_ENDPOINT_URI = "federator.iwsn.sm_endpoint_uri";

	@Inject
	@Named(FEDERATOR_SM_ENDPOINT_URI)
	private URI federatorSmEndpointUri;

	@PropConf(
			usage = "The Endpoint URI of the federator RS service",
			example = "http://federator.mydomain.tld/rs",
			typeConverter = URITypeConverter.class
	)
	public static final String FEDERATOR_RS_ENDPOINT_URI = "federator.iwsn.rs_endpoint_uri";

	@Inject
	@Named(FEDERATOR_RS_ENDPOINT_URI)
	private URI federatorRsEndpointUri;

	@PropConf(
			usage = "The Endpoint URI of the federator SNAA service",
			example = "http://federator.mydomain.tld/snaa",
			typeConverter = URITypeConverter.class
	)
	public static final String FEDERATOR_SNAA_ENDPOINT_URI = "federator.iwsn.snaa_endpoint_uri";

	@Inject
	@Named(FEDERATOR_SNAA_ENDPOINT_URI)
	private URI federatorSnaaEndpointUri;

	public URI getWsnEndpointUriBase() {
		return wsnEndpointUriBase;
	}

	public String getControllerContextPathBase() {
		return controllerContextPathBase;
	}

	public URIToNodeUrnPrefixSetMap getFederates() {
		return federates;
	}

	public URI getFederatorSmEndpointUri() {
		return federatorSmEndpointUri;
	}

	public URI getFederatorRsEndpointUri() {
		return federatorRsEndpointUri;
	}

	public URI getFederatorSnaaEndpointUri() {
		return federatorSnaaEndpointUri;
	}
}
