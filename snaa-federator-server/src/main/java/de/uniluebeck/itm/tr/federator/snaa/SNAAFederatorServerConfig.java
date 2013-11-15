package de.uniluebeck.itm.tr.federator.snaa;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;

public class SNAAFederatorServerConfig {

	@PropConf(
			usage = "Port on which to run the SNAA federator",
			example = "8883",
			defaultValue = "8883"
	)
	public static final String FEDERATOR_PORT = "federator.snaa.port";

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
