package de.uniluebeck.itm.wisebed.cmdlineclient;

public class AuthenticationCredentials {

	private final String urnPrefix;

	private final String username;

	private final String password;

	public AuthenticationCredentials(final String urnPrefix, final String username, final String password) {
		this.password = password;
		this.urnPrefix = urnPrefix;
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public String getUrnPrefix() {
		return urnPrefix;
	}

	public String getUsername() {
		return username;
	}
}
