package de.uniluebeck.itm.tr.common;

import java.net.URI;

public interface EndpointManager {

	URI getSnaaEndpointUri();

	URI getRsEndpointUri();

	URI getSmEndpointUri();

	URI getWsnEndpointUriBase();
}
