package de.uniluebeck.itm.tr.common.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.util.propconf.PropConf;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.TimeZone;

public class CommonConfig {

	@PropConf(
			usage = "DNS-resolvable hostname on which this process and its services will be run"
	)
	public static final String HOSTNAME = "hostname";

	@Inject
	@Named(HOSTNAME)
	protected String hostname;

	@PropConf(
			usage = "Publicly accessible port under which this process and its services shall be available"
	)
	public static final String PORT = "port";

	@Inject
	@Named(PORT)
	protected int port;

	@PropConf(
			usage = "Username for the administrator",
			defaultValue = "admin"
	)
	public static final String ADMIN_USERNAME = "admin.username";

	@Inject
	@Named(ADMIN_USERNAME)
	protected String adminUsername;

	@PropConf(
			usage = "Password for the administrator",
			defaultValue = "secret"
	)
	public static final String ADMIN_PASSWORD = "admin.password";

	@Inject
	@Named(ADMIN_PASSWORD)
	protected String adminPassword;

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

	public String getHostname() {
		return hostname;
	}

	public void setHostname(final String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public TimeZone getTimeZone() {
		return TimeZone.getTimeZone(timeZone);
	}

	public NodeUrnPrefix getUrnPrefix() {
		return urnPrefix;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void setTimeZone(final String timeZone) {
		this.timeZone = timeZone;
	}

	public void setUrnPrefix(final NodeUrnPrefix urnPrefix) {
		this.urnPrefix = urnPrefix;
	}

	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(final String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(final String adminPassword) {
		this.adminPassword = adminPassword;
	}
}
