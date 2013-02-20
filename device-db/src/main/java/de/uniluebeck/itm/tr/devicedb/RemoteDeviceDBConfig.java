package de.uniluebeck.itm.tr.devicedb;

import org.kohsuke.args4j.Option;

import java.net.URI;

public class RemoteDeviceDBConfig {

	public RemoteDeviceDBConfig() {
	}

	public RemoteDeviceDBConfig(final URI uri) {
		this.uri = uri;
	}

	@Option(
			name = "--uri",
			usage = "The URI the DeviceDB REST service runs on",
			required = true
	)
	public URI uri = null;

}
