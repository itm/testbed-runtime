package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import de.uniluebeck.itm.util.propconf.converters.MultimapTypeConverter;
import de.uniluebeck.itm.util.propconf.converters.URITypeConverter;

import javax.annotation.Nullable;
import java.net.URI;

public class PortalServerConfig {

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
			usage = "The directory in which to save experiment outputs and events",
			example = "/var/log/tr.iwsn-portal/events"
	)
	public static final String EVENTSTORE_PATH = "eventstore.path";

	@Inject
	@Named(EVENTSTORE_PATH)
	private String eventStorePath;

    @PropConf(
            usage = "The directory in which to save json event files to download",
            example = "/var/log/tr.iwsn-portal/eventdownloads"
    )
    public static final String EVENTSTORE_DOWNLOAD_PATH = "eventstore.download_path";

    @Inject
    @Named(EVENTSTORE_DOWNLOAD_PATH)
    private String eventStoreDownloadPath;


	@PropConf(
			usage = "The DNS-resolvable endpoint URI of the reservation system (RS) service (to be returned by SessionManagement.getConfiguration())",
			example = "http://portal.mydomain.tld/soap/v3/rs",
			typeConverter = URITypeConverter.class
	)
	public static final String CONFIGURATION_RS_ENDPOINT_URI = "portal.configuration.rs_endpoint_uri";

	@Inject
	@Named(CONFIGURATION_RS_ENDPOINT_URI)
	private URI configurationRsEndpointUri;

	@PropConf(
			usage = "The DNS-resolvable endpoint URI of the authentication and authorization (SNAA) service  (to be returned by SessionManagement.getConfiguration())",
			example = "http://portal.mydomain.tld/soap/v3/snaa",
			typeConverter = URITypeConverter.class
	)
	public static final String CONFIGURATION_SNAA_ENDPOINT_URI = "portal.configuration.snaa_endpoint_uri";

	@Inject
	@Named(CONFIGURATION_SNAA_ENDPOINT_URI)
	private URI configurationSnaaEndpointUri;

	@PropConf(
			usage = "The DNS-resolvable endpoint URI of the Session Management (SM) service (to be returned by SessionManagement.getConfiguration())",
			example = "http://portal.mydomain.tld/soap/v3/sm",
			typeConverter = URITypeConverter.class
	)
	public static final String CONFIGURATION_SM_ENDPOINT_URI = "portal.configuration.sm_endpoint_uri";

	@Inject
	@Named(CONFIGURATION_SM_ENDPOINT_URI)
	private URI configurationSmEndpointUri;

	@PropConf(
			usage = "The endpoint URL of the WSN service instances",
			example = "http://portal.mydomain.tld/soap/v3/wsn",
			typeConverter = URITypeConverter.class
	)
	public static final String CONFIGURATION_WSN_ENDPOINT_URI_BASE = "portal.configuration.wsn_endpoint_uri_base";

	@Inject
	@Named(CONFIGURATION_WSN_ENDPOINT_URI_BASE)
	private URI configurationWsnEndpointUriBase;

	@PropConf(
			usage = "Additional key/value pairs to be returned by SessionManagements.getConfiguration(). "
					+ "Multiple comma-separated values are allowed per key.",
			example = "k1=k1v1 k2=k2v1,k2v2",
			typeConverter = MultimapTypeConverter.class
	)
	public static final String CONFIGURATION_OPTIONS = "portal.configuration.options";

	@Inject(optional = true)
	@Named(CONFIGURATION_OPTIONS)
	private Multimap<String, String> configurationOptions;

	@PropConf(
			usage = "Path to a directory which Testbed Runtime will check for plugins"
	)
	public static final String PLUGIN_DIRECTORY = "portal.plugin_directory";

	@Inject(optional = true)
	@Named(PLUGIN_DIRECTORY)
	private String pluginDirectory;

	public int getOverlayPort() {
		return overlayPort;
	}

	public String getEventStorePath() {
		return eventStorePath;
	}

	public URI getConfigurationRsEndpointUri() {
		return configurationRsEndpointUri;
	}

	public Multimap<String, String> getConfigurationOptions() {
		return configurationOptions == null ? HashMultimap.<String, String>create() : configurationOptions;
	}

	public URI getConfigurationSnaaEndpointUri() {
		return configurationSnaaEndpointUri;
	}

	public URI getConfigurationSmEndpointUri() {
		return configurationSmEndpointUri;
	}

	public URI getConfigurationWsnEndpointUriBase() {
		return configurationWsnEndpointUriBase;
	}

	@Nullable
	public String getPluginDirectory() {
		return pluginDirectory;
	}
}
