package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.config.MultimapTypeConverter;
import de.uniluebeck.itm.tr.common.config.URITypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import java.net.URI;

public class PortalConfig {

	@PropConf(
			usage = "Port to listen on for the internal overlay network",
			example = "8880",
			defaultValue = "8880"
	)
	public static final String OVERLAY_PORT = "portal.overlay.port";

	@Inject
	@Named(OVERLAY_PORT)
	private int overlayPort;

	@PropConf(
			usage = "Port to provide the protobuf-based API on",
			example = "8885"
	)
	public static final String PROTOBUF_API_PORT = "portal.protobuf.api.port";

	@Inject(optional = true)
	@Named(PROTOBUF_API_PORT)
	private int protobufApiPort;

	@PropConf(
			usage = "The endpoint URI of the reservation system (RS) service",
			example = "http://portal.mydomain.tld/rs",
			typeConverter = URITypeConverter.class
	)
	public static final String RS_ENDPOINT_URI = "portal.rs.endpoint_uri";

	@Inject
	@Named(RS_ENDPOINT_URI)
	private URI rsEndpointUri;

	@PropConf(
			usage = "The endpoint URL of the authentication and authorization (SNAA) service",
			example = "http://portal.mydomain.tld/snaa",
			typeConverter = URITypeConverter.class
	)
	public static final String SNAA_ENDPOINT_URI = "portal.snaa.endpoint_uri";

	@Inject
	@Named(SNAA_ENDPOINT_URI)
	private URI snaaEndpointUri;

	@PropConf(
			usage = "The endpoint URL of the Session Management (SM) service",
			example = "http://portal.mydomain.tld/sm",
			typeConverter = URITypeConverter.class
	)
	public static final String SM_ENDPOINT_URI = "portal.sm.endpoint_uri";

	@Inject
	@Named(SM_ENDPOINT_URI)
	private URI smEndpointUri;

	@PropConf(
			usage = "Additional key/value pairs to be returned by SessionManagements.getConfiguration(). "
					+ "Multiple comma-separated values are allowed per key.",
			example = "k1=k1v1 k2=k2v1,k2v2",
			typeConverter = MultimapTypeConverter.class
	)
	public static final String SM_CONFIGURATION = "portal.sm.configuration";

	@Inject(optional = true)
	@Named(SM_CONFIGURATION)
	private Multimap<String, String> smConfiguration;

	@PropConf(
			usage = "Name of the testbed to be displayed in WiseGui frontend",
			defaultValue = "local testbed"
	)
	public static final String WISEGUI_TESTBED_NAME = "portal.wisegui.testbedname";

	@Inject
	@Named(WISEGUI_TESTBED_NAME)
	private String wiseguiTestbedName;

	public int getOverlayPort() {
		return overlayPort;
	}

	public int getProtobufApiPort() {
		return protobufApiPort;
	}

	public URI getRsEndpointUri() {
		return rsEndpointUri;
	}

	public Multimap<String, String> getSmConfiguration() {
		return smConfiguration == null ? HashMultimap.<String, String>create() : smConfiguration;
	}

	public URI getSnaaEndpointUri() {
		return snaaEndpointUri;
	}

	public URI getSmEndpointUri() {
		return smEndpointUri;
	}

	public String getWiseguiTestbedName() {
		return wiseguiTestbedName;
	}
}
