package de.uniluebeck.itm.tr.devicedb;

import de.uniluebeck.itm.tr.iwsn.common.config.ConfigWithLogging;
import org.kohsuke.args4j.Option;

import java.io.File;

public class DeviceDBServiceConfig extends ConfigWithLogging {

	public DeviceDBServiceConfig() {
	}

	public DeviceDBServiceConfig(final int port, final File dbPropertiesFile) {
		this.port = port;
		this.dbPropertiesFile = dbPropertiesFile;
	}

	@Option(
			name = "--port",
			usage = "The port for the REST API to run on (default: 8080)",
			required = false
	)
	public int port = 8080;

	@Option(
			name = "--dbPropertiesFile",
			usage = ".properties file to initialize JPA",
			required = true
	)
	public File dbPropertiesFile = null;
}
