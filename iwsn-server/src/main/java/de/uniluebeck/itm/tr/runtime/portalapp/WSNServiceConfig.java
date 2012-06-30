package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableSet;
import eu.wisebed.wiseml.Wiseml;

import java.net.URL;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class WSNServiceConfig {

	/**
	 * The endpoint URL of this WSN service instance.
	 */
	private final URL webserviceEndpointUrl;

	/**
	 * The WiseML document that is delivered when {@link WSNServiceImpl#getNetwork()} is called.
	 */
	private final Wiseml wiseML;

	/**
	 * The set of node URNs that are reserved and thereby associated with this {@link eu.wisebed.api.wsn.WSN} instance.
	 */
	private final ImmutableSet<String> reservedNodes;

	public WSNServiceConfig(final ImmutableSet<String> reservedNodes, final URL webServiceEndpointUrl,
							final Wiseml wiseML) {

		checkNotNull(reservedNodes);
		checkNotNull(webServiceEndpointUrl);
		checkNotNull(wiseML);

		this.reservedNodes = reservedNodes;
		this.webserviceEndpointUrl = webServiceEndpointUrl;
		this.wiseML = wiseML;
	}

	public ImmutableSet<String> getReservedNodes() {
		return reservedNodes;
	}

	public Wiseml getWiseML() {
		return wiseML;
	}

	public URL getWebserviceEndpointUrl() {
		return webserviceEndpointUrl;
	}
}
