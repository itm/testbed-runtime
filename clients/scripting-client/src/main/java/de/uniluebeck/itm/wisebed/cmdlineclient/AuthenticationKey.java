package de.uniluebeck.itm.wisebed.cmdlineclient;

public class AuthenticationKey {

	private final String urnPrefix;

	private final String username;

	private final String secretAuthenticationKey;

	public AuthenticationKey(final String urnPrefix, final String username, final String secretAuthenticationKey) {
		this.urnPrefix = urnPrefix;
		this.username = username;
		this.secretAuthenticationKey = secretAuthenticationKey;
	}

	public String getSecretAuthenticationKey() {
		return secretAuthenticationKey;
	}

	public String getUrnPrefix() {
		return urnPrefix;
	}

	public String getUsername() {
		return username;
	}
}
