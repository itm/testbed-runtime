package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.wiseml.Wiseml;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class SessionManagementWisemlProvider implements Provider<Wiseml> {

	private final FederatedEndpoints<SessionManagement> federatedEndpoints;

	private final ExecutorService executorService;

	@Inject
	public SessionManagementWisemlProvider(
			final FederatedEndpoints<SessionManagement> federatedEndpoints, final ExecutorService executorService) {
		this.federatedEndpoints = federatedEndpoints;
		this.executorService = executorService;
	}

	@Override
	public Wiseml get() {
		final BiMap<URI, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		for (final FederatedEndpoints.Entry<SessionManagement> entry : federatedEndpoints.getEntries()) {
			endpointUrlToCallableMap.put(entry.endpointUrl, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return entry.endpoint.getNetwork();
				}
			}
			);
		}
		return FederatorWiseMLMerger.merge(endpointUrlToCallableMap, executorService);
	}
}
