package de.uniluebeck.itm.tr.federator;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

public class FederatorServerConfig {

	@PropConf(
			usage = "Port on which to run the federator",
			example = "8880",
			defaultValue = "8880"
	)
	public static final String FEDERATOR_PORT = "federator.port";

	@Inject
	@Named(FEDERATOR_PORT)
	private int port;

	@PropConf(
			usage = "Shiro INI file (if given, configures the Apache Shiro framework to do authentication and authorization for all published REST, SOAP & HTML services)",
			defaultValue = ""
	)
	public static final String SHIRO_INI = "shiro.ini";

	@Inject(optional = true)
	@Named(SHIRO_INI)
	protected String shiroIni;

	public int getPort() {
		return port;
	}

	public String getShiroIni() {
		return shiroIni;
	}
}
