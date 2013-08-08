package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import de.uniluebeck.itm.util.propconf.converters.HostAndPortTypeConverter;

import javax.annotation.Nullable;

public class GatewayConfig {

	@PropConf(
			usage = "Hostname and port on which the portal server listens for the internal network "
					+ "(e.g. $PORTAL_HOSTNAME:8880)",
			typeConverter = HostAndPortTypeConverter.class
	)
	public static final String PORTAL_ADDRESS = "gateway.portaladdress";

	@Inject
	@Named(PORTAL_ADDRESS)
	private HostAndPort portalAddress;

	@PropConf(
			usage = "If set to true, a REST API is started",
			defaultValue = "false"
	)
	public static final String REST_API_START = "gateway.restapi.start";

	@Inject
	@Named(REST_API_START)
	private boolean restAPI;

	@PropConf(
			usage = "The port for the REST API to run on (only used when --restAPI is set)",
			defaultValue = "8080"
	)
	public static final String REST_API_PORT = "gateway.restapi.port";

	@Inject
	@Named(REST_API_PORT)
	private int restAPIPort;

	@PropConf(
			usage = "Path to a directory which Testbed Runtime will check for plugins"
	)
	public static final String PLUGIN_DIRECTORY = "gateway.plugin_directory";

	@Inject(optional = true)
	@Named(PLUGIN_DIRECTORY)
	private String pluginDirectory;

	public HostAndPort getPortalAddress() {
		return portalAddress;
	}

	public boolean isRestAPI() {
		return restAPI;
	}

	public int getRestAPIPort() {
		return restAPIPort;
	}

	@Nullable
	public String getPluginDirectory() {
		return pluginDirectory;
	}
}
