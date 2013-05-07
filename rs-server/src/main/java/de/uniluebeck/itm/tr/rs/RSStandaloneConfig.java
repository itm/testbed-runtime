package de.uniluebeck.itm.tr.rs;

import java.net.URI;

public interface RSStandaloneConfig extends RSConfig {

	int getPort();

	URI getSmEndpointUri();

	URI getSnaaEndpointUri();

}
