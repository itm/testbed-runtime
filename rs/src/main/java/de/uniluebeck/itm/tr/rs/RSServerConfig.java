package de.uniluebeck.itm.tr.rs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import java.net.URI;

public class RSServerConfig {

	@PropConf(
			usage = "Endpoint URI of the SNAA service",
			typeConverter = URITypeConverter.class
	)
	public static final String SNAA_ENDPOINT_URI = "snaa_endpoint_uri";

	@Inject
	@Named(SNAA_ENDPOINT_URI)
	private URI snaaEndpointUri;

	@PropConf(
			usage = "Endpoint URI of the session management (SM) service",
			typeConverter = URITypeConverter.class
	)
	public static final String SM_ENDPOINT_URI = "sm_endpoint_uri";

	@Inject
	@Named(SM_ENDPOINT_URI)
	private URI smEndpointUri;

	public URI getSmEndpointUri() {
		return smEndpointUri;
	}

	public URI getSnaaEndpointUri() {
		return snaaEndpointUri;
	}
}
