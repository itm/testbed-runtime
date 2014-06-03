package de.uniluebeck.itm.tr.federator;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

public class FederatorServerConfig {

	@PropConf(
			usage = "DNS-resolvable hostname on which this process and its services will be run"
	)
	public static final String FEDERATOR_HOSTNAME = "federator.hostname";

	@Inject
	@Named(FEDERATOR_HOSTNAME)
	private String federatorHostname;

	public String getFederatorHostname() {
		return federatorHostname;
	}

	@PropConf(
			usage = "Publicly accessible port under which this process and its services shall be available",
			example = "8888",
			defaultValue = "8888"
	)
	public static final String FEDERATOR_PORT = "federator.port";

	@Inject
	@Named(FEDERATOR_PORT)
	private int federatorPort;

	public int getFederatorPort() {
		return federatorPort;
	}
}
