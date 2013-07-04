package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.collect.ImmutableSet;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.MalformedURLException;
import java.net.URI;

public class FederatorWSNTestbedConfig {

	private final URI smEndpointUrl;

	private final ImmutableSet<NodeUrnPrefix> urnPrefixes;

	public FederatorWSNTestbedConfig(final URI smEndpointUrl, final ImmutableSet<NodeUrnPrefix> urnPrefixes) {

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

	public ImmutableSet<NodeUrnPrefix> getUrnPrefixes() {
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