package de.uniluebeck.itm.tr.federator.utils;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.sm.SessionManagement;

import java.util.Set;

public class FederationManagerServedNodeUrnPrefixesProvider implements ServedNodeUrnPrefixesProvider {

	private final FederatedEndpoints<SessionManagement> federatedEndpoints;

	@Inject
	public FederationManagerServedNodeUrnPrefixesProvider(
			final FederatedEndpoints<SessionManagement> federatedEndpoints) {
		this.federatedEndpoints = federatedEndpoints;
	}

	@Override
	public Set<NodeUrnPrefix> get() {
		//noinspection unchecked
		return federatedEndpoints.getUrnPrefixes();
	}
}
