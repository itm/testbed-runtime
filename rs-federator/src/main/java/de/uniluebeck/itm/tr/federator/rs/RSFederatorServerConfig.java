package de.uniluebeck.itm.tr.federator.rs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

public class RSFederatorServerConfig {

	@PropConf(
			usage = "Port on which to run the RS federator",
			example = "8882",
			defaultValue = "8882"
	)
	public static final String FEDERATOR_PORT = "federator.rs.port";

	@Inject
	@Named(FEDERATOR_PORT)
	private int port;

	public int getPort() {
		return port;
	}
}
