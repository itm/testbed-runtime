package de.uniluebeck.itm.tr.devicedb;

import de.uniluebeck.itm.tr.iwsn.common.config.ConfigWithLogging;
import de.uniluebeck.itm.tr.iwsn.common.config.PropertiesOptionHandler;
import org.kohsuke.args4j.Option;

import java.util.Properties;

public class DeviceDBMainConfig extends ConfigWithLogging {

	public DeviceDBMainConfig() {
	}

	public DeviceDBMainConfig(final int port, final Properties dbProperties) {
		this.port = port;
		this.dbProperties = dbProperties;
	}

	@Option(
			name = "--port",
			usage = "The port for the REST API to run on (default: 8080)",
			required = false
	)
	public int port = 8080;

	@Option(
			name = "--dbProperties",
			usage = ".properties file to initialize JPA",
			required = true,
			handler = PropertiesOptionHandler.class
	)
	public Properties dbProperties = null;
}
