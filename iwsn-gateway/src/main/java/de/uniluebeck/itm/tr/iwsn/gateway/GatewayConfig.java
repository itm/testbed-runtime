package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.config.HostAndPortTypeConverter;
import de.uniluebeck.itm.tr.common.config.URITypeConverter;
import de.uniluebeck.itm.util.propconf.PropConf;

import java.net.URI;

public class GatewayConfig {

	@PropConf(
			usage = "Hostname and port on which the portal server listens for the internal network "
					+ "(e.g. $PORTAL_HOSTNAME:8880)",
			required = true,
			typeConverter = HostAndPortTypeConverter.class
	)
	public static final String PORTAL_ADDRESS = "gateway.portaladdress";

	@PropConf(
			usage = "If set to true, a REST API is started",
			required = false,
			defaultValue = "false"
	)
	public static final String REST_API_START = "gateway.restapi.start";

	@PropConf(
			usage = "The port for the REST API to run on (only used when --restAPI is set)",
			required = false,
			defaultValue = "8080"
	)
	public static final String REST_API_PORT = "gateway.restapi.port";

	@PropConf(
			usage = "The URI on which the DeviceDB runs",
			required = true,
			typeConverter = URITypeConverter.class
	)
	public static final String DEVICE_DB_URI = "gateway.devicedb.uri";

	@Inject
	@Named(PORTAL_ADDRESS)
	private HostAndPort portalAddress;

	@Inject
	@Named(REST_API_START)
	private boolean restAPI;

	@Inject
	@Named(REST_API_PORT)
	private int restAPIPort;

	@Inject
	@Named(DEVICE_DB_URI)
	private URI deviceDBUri;

	public URI getDeviceDBUri() {
		return deviceDBUri;
	}

	public HostAndPort getPortalAddress() {
		return portalAddress;
	}

	public boolean isRestAPI() {
		return restAPI;
	}

	public int getRestAPIPort() {
		return restAPIPort;
	}
}
