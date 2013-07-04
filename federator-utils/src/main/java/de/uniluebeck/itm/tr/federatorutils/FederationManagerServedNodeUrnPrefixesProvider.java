package de.uniluebeck.itm.tr.federatorutils;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.sm.SessionManagement;

import java.util.Set;

public class FederationManagerServedNodeUrnPrefixesProvider implements ServedNodeUrnPrefixesProvider {

	private final FederationManager<SessionManagement> federationManager;

	@Inject
	public FederationManagerServedNodeUrnPrefixesProvider(
			final FederationManager<SessionManagement> federationManager) {
		this.federationManager = federationManager;
	}

	@Override
	public Set<NodeUrnPrefix> get() {
		//noinspection unchecked
		return federationManager.getUrnPrefixes();
	}
}
