package de.uniluebeck.itm.tr.common.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.TimeZone;

public class CommonConfig {

	@PropConf(
			usage = "Port to run all services on",
			defaultValue = "9999"
	)
	public static final String PORT = "port";

	@Inject
	@Named(PORT)
	protected int port;

	@PropConf(
			usage = "The URN prefix of this testbed (e.g. \"urn:wisebed:uzl1:\")",
			typeConverter = NodeUrnPrefixTypeConverter.class
	)
	public static final String URN_PREFIX = "urnprefix";

	@Inject
	@Named(URN_PREFIX)
	protected NodeUrnPrefix urnPrefix;

	@PropConf(
			usage = "The local timezone",
			defaultValue = "GMT"
	)
	public static final String TIMEZONE = "timezone";

	@Inject
	@Named(TIMEZONE)
	private String timeZone;

	public int getPort() {
		return port;
	}

	public TimeZone getTimeZone() {
		return TimeZone.getTimeZone(timeZone);
	}

	public NodeUrnPrefix getUrnPrefix() {
		return urnPrefix;
	}
}
