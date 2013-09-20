package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import de.uniluebeck.itm.util.propconf.converters.HostAndPortTypeConverter;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;

import javax.annotation.Nullable;
import java.net.URI;

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

	@PropConf(
			usage = "If run in the context of the SmartSantander project, this must be set to the URI on which the " +
					"SmartSantander EventBroker runs ",
			example = "failover://(tcp://localhost:9009)?startupMaxReconnectAttempts=1&initialReconnectDelay=1",
			typeConverter = URITypeConverter.class
	)
	public static final String SMARTSANTANDER_EVENT_BROKER_URI = "gateway.smartsantander.event_broker.uri";

	@Inject(optional = true)
	@Named(SMARTSANTANDER_EVENT_BROKER_URI)
	private URI smartSantanderEventBrokerUri;

	@PropConf(
			usage = "If run in the context of the SmartSantander project, this must be set to the gateways ID that is "
					+ "used to identify the gateway machine in the EventBroker messages"
	)
	public static final String SMARTSANTANDER_GATEWAY_ID = "gateway.smartsantander.gateway_id";

	@Inject(optional = true)
	@Named(SMARTSANTANDER_GATEWAY_ID)
	private String smartSantanderGatewayId;

	@PropConf(
			usage = "Shiro INI file (if given, configures the Apache Shiro framework to do authentication and authorization for all published REST, SOAP & HTML services)",
			defaultValue = ""
	)
	public static final String SHIRO_INI = "shiro.ini";

	@Inject(optional = true)
	@Named(SHIRO_INI)
	protected String shiroIni;

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

	public URI getSmartSantanderEventBrokerUri() {
		return smartSantanderEventBrokerUri;
	}

	public String getSmartSantanderGatewayId() {
		return smartSantanderGatewayId;
	}

	public String getShiroIni() {
		return shiroIni;
	}
}