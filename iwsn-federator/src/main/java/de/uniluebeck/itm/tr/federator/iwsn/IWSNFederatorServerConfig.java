package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

public class IWSNFederatorServerConfig {

	@PropConf(
			usage = "Port on which to run the iWSN federator",
			example = "8881",
			defaultValue = "8881"
	)
	public static final String FEDERATOR_PORT = "federator.iwsn.port";

	@Inject
	@Named(FEDERATOR_PORT)
	private int port;

	public int getPort() {
		return port;
	}
}
