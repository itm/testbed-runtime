package de.uniluebeck.itm.tr.federatorutils;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.SessionManagement;

import java.util.Set;

import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.STRING_TO_NODE_URN;
import static eu.wisebed.wiseml.WiseMLHelper.getNodeUrns;

public class FederationManagerServedNodeUrnsProvider implements ServedNodeUrnsProvider {

	private final FederationManager<SessionManagement> federationManager;

	@Inject
	public FederationManagerServedNodeUrnsProvider(final FederationManager<SessionManagement> federationManager) {
		this.federationManager = federationManager;
	}

	@Override
	public Set<NodeUrn> get() {
		final Set<NodeUrn> set = newHashSet();
		for (SessionManagement sm : federationManager.getEndpoints()) {
			set.addAll(transform(getNodeUrns(sm.getNetwork()), STRING_TO_NODE_URN));
		}
		return set;
	}
}
