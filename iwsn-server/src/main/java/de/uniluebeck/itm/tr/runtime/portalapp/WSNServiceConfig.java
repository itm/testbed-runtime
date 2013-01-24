package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableSet;
import eu.wisebed.api.v3.common.NodeUrn;
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
	 * The set of node URNs that are reserved and thereby associated with this {@link eu.wisebed.api.v3.wsn.WSN} instance.
	 */
	private final ImmutableSet<NodeUrn> reservedNodes;

	public WSNServiceConfig(final ImmutableSet<NodeUrn> reservedNodes,
							final URL webServiceEndpointUrl,
							final Wiseml wiseML) {
		this.reservedNodes = checkNotNull(reservedNodes);
		this.webserviceEndpointUrl = checkNotNull(webServiceEndpointUrl);
		this.wiseML = checkNotNull(wiseML);
	}

	public ImmutableSet<NodeUrn> getReservedNodes() {
		return reservedNodes;
	}

	public Wiseml getWiseML() {
		return wiseML;
	}

	public URL getWebserviceEndpointUrl() {
		return webserviceEndpointUrl;
	}
}
