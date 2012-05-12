package de.uniluebeck.itm.tr.wsn.federator;

import com.google.common.collect.ImmutableSet;

import java.net.MalformedURLException;
import java.net.URI;

public class FederatorWSNTestbedConfig {

	private final URI smEndpointUrl;

	private final ImmutableSet<String> urnPrefixes;

	public FederatorWSNTestbedConfig(final URI smEndpointUrl, final ImmutableSet<String> urnPrefixes) {

		try {
			smEndpointUrl.toURL();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}

		this.smEndpointUrl = smEndpointUrl;
		this.urnPrefixes = urnPrefixes;
	}

	public URI getSmEndpointUrl() {
		return smEndpointUrl;
	}

	public ImmutableSet<String> getUrnPrefixes() {
		return urnPrefixes;
	}

	@Override
	public String toString() {
		return "FederatorWSNTestbedConfig{" +
				"smEndpointUrl='" + smEndpointUrl + '\'' +
				", urnPrefixes=" + urnPrefixes +
				"}";
	}
}