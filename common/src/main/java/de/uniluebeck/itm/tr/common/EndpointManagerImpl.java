package de.uniluebeck.itm.tr.common;

import javax.annotation.Nullable;
import java.net.URI;

public class EndpointManagerImpl implements EndpointManager {

	private final URI snaaEndpointUri;

	private final URI rsEndpointUri;

	private final URI smEndpointUri;

	private final URI wsnEndpointBaseUri;

	public EndpointManagerImpl(@Nullable final URI rsEndpointUri,
							   @Nullable final URI snaaEndpointUri,
							   @Nullable final URI smEndpointUri,
							   @Nullable final URI wsnEndpointBaseUri) {
		this.rsEndpointUri = rsEndpointUri;
		this.snaaEndpointUri = snaaEndpointUri;
		this.smEndpointUri = smEndpointUri;
		this.wsnEndpointBaseUri = wsnEndpointBaseUri;
	}

	public URI getRsEndpointUri() {
		return rsEndpointUri;
	}

	public URI getSmEndpointUri() {
		return smEndpointUri;
	}

	@Override
	public URI getWsnEndpointUriBase() {
		return wsnEndpointBaseUri;
	}

	public URI getSnaaEndpointUri() {
		return snaaEndpointUri;
	}
}
