package de.uniluebeck.itm.tr.federator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.WisemlProvider;
import de.uniluebeck.itm.tr.federator.iwsn.FederatorWiseMLMerger;
import de.uniluebeck.itm.tr.federator.utils.FederationManager;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.wiseml.WiseMLHelper;
import eu.wisebed.wiseml.Wiseml;

import java.net.URI;
import java.util.concurrent.Callable;

public class FederatorWiseMLProvider implements WisemlProvider {

	private final FederationManager<SessionManagement> federationManager;

	@Inject
	public FederatorWiseMLProvider(
			final FederationManager<SessionManagement> federationManager) {
		this.federationManager = federationManager;
	}

	@Override
	public Wiseml get() {

		final BiMap<URI, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		for (final FederationManager.Entry<SessionManagement> entry : federationManager.getEntries()) {
			endpointUrlToCallableMap.put(entry.endpointUrl, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return entry.endpoint.getNetwork();
				}
			}
			);
		}

		return WiseMLHelper.deserialize(
				FederatorWiseMLMerger.merge(endpointUrlToCallableMap, MoreExecutors.sameThreadExecutor())
		);
	}

	@Override
	public Wiseml get(final Iterable<NodeUrn> nodeUrns) {
		throw new RuntimeException("Implement me!");
	}
}
