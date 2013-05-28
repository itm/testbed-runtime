package de.uniluebeck.itm.tr.snaa.shibboleth;

public class ShibbolethProxy {

	private String proxyHost;

	private int proxyPort;

	public ShibbolethProxy(String proxyHost, int proxyPort) {
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public String getProxyHost() {
		return proxyHost;
	}
}
