package de.uniluebeck.itm.tr.federator.utils;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.SessionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.STRING_TO_NODE_URN;
import static eu.wisebed.wiseml.WiseMLHelper.getNodeUrns;

public class FederationManagerServedNodeUrnsProvider implements ServedNodeUrnsProvider {

	private static final Logger log = LoggerFactory.getLogger(FederationManagerServedNodeUrnsProvider.class);

	private final FederationManager<SessionManagement> federationManager;

	@Inject
	public FederationManagerServedNodeUrnsProvider(final FederationManager<SessionManagement> federationManager) {
		this.federationManager = federationManager;
	}

	@Override
	public Set<NodeUrn> get() {
		final Set<NodeUrn> set = newHashSet();
		for (final Map.Entry<SessionManagement, URI> entry : federationManager.getEndpointsURIMap().entrySet()) {
			try {
				set.addAll(transform(getNodeUrns(entry.getKey().getNetwork()), STRING_TO_NODE_URN));
			} catch (Exception e) {
				log.error("Exception while fetching node URNs from endpoint URI {}: {}",
						entry.getValue(),
						getStackTraceAsString(e)
				);
				throw propagate(e);
			}
		}
		return set;
	}
}
