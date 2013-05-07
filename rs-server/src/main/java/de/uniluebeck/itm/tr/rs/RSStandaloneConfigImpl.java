package de.uniluebeck.itm.tr.rs;

import org.kohsuke.args4j.Option;

import java.net.URI;

public class RSStandaloneConfigImpl extends RSConfigImpl implements RSStandaloneConfig {

	@Option(name = "--snaaEndpointUri",
			usage = "Endpoint URI of the SNAA service",
			required = true)
	private URI snaaEndpointUri;

	@Option(name = "--smEndpointUri",
			usage = "Endpoint URI of the session management (SM) service",
			required = true)
	private URI smEndpointUri;

	@Override
	public URI getSmEndpointUri() {
		return smEndpointUri;
	}

	@Override
	public URI getSnaaEndpointUri() {
		return snaaEndpointUri;
	}

	@SuppressWarnings("unused")
	public void setSmEndpointUri(final URI smEndpointUri) {
		this.smEndpointUri = smEndpointUri;
	}

	@SuppressWarnings("unused")
	public void setSnaaEndpointUri(final URI snaaEndpointUri) {
		this.snaaEndpointUri = snaaEndpointUri;
	}
}
