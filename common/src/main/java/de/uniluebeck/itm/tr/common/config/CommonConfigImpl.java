package de.uniluebeck.itm.tr.common.config;

import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.Option;

import java.util.TimeZone;

public class CommonConfigImpl extends ConfigWithLogging implements CommonConfig {

	@Option(name = "--port",
			usage = "Port to provide all user services on (default: 8888)")
	protected int port = 8888;

	@Option(name = "--urnPrefix",
			usage = "The URN prefix of this testbed (e.g. \"urn:wisebed:uzl1:\")",
			handler = NodeUrnPrefixOptionHandler.class,
			required = true)
	protected NodeUrnPrefix urnPrefix;

	@Option(name = "--timezone",
			usage = "Time zone of the RS (default: default timezone)",
			handler = TimeZoneOptionHandler.class)
	private TimeZone timeZone = TimeZone.getDefault();

	@Override
	public int getPort() {
		return port;
	}

	@SuppressWarnings("unused")
	public void setPort(final int port) {
		this.port = port;
	}

	@Override
	public TimeZone getTimeZone() {
		return timeZone;
	}

	@SuppressWarnings("unused")
	public void setTimeZone(final TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	@Override
	public NodeUrnPrefix getUrnPrefix() {
		return urnPrefix;
	}

	@SuppressWarnings("unused")
	public void setUrnPrefix(final NodeUrnPrefix urnPrefix) {
		this.urnPrefix = urnPrefix;
	}
}
