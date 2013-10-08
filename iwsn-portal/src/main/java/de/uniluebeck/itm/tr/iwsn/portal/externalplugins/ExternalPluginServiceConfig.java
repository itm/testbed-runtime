package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

import javax.annotation.Nullable;

public class ExternalPluginServiceConfig {

	@PropConf(
			usage = "Port to listen on for external plugins to connect to",
			example = "8882"
	)
	public static final String EXTERNAL_PLUGIN_SERVICE_PORT = "portal.external_plugin_service.port";

	@Nullable
	@Inject(optional = true)
	@Named(EXTERNAL_PLUGIN_SERVICE_PORT)
	private Integer externalPluginServicePort;

	@Nullable
	public Integer getExternalPluginServicePort() {
		return externalPluginServicePort;
	}
}
