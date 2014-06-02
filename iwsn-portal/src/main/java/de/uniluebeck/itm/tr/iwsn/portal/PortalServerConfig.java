package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import de.uniluebeck.itm.util.propconf.converters.MultimapTypeConverter;

import javax.annotation.Nullable;

public class PortalServerConfig {

	@PropConf(
			usage = "Port to listen on for the gateway hosts to connect to",
			example = "8880",
			defaultValue = "8880"
	)
	public static final String GATEWAY_PORT = "portal.gateway_port";

	@Inject
	@Named(GATEWAY_PORT)
	private int gatewayPort;

	@PropConf(
			usage = "The directory in which to save experiment outputs and events",
			example = "/var/log/tr.iwsn-portal/eventstore"
	)
	public static final String EVENTSTORE_PATH = "eventstore.path";

	@Inject
	@Named(EVENTSTORE_PATH)
	private String eventStorePath;

	@PropConf(
			usage = "Additional key/value pairs to be returned by SessionManagement.getConfiguration(). "
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

	public int getGatewayPort() {
		return gatewayPort;
	}

	public String getEventStorePath() {
		return eventStorePath;
	}

	public Multimap<String, String> getConfigurationOptions() {
		return configurationOptions == null ? HashMultimap.<String, String>create() : configurationOptions;
	}

	@Nullable
	public String getPluginDirectory() {
		return pluginDirectory;
	}
}
