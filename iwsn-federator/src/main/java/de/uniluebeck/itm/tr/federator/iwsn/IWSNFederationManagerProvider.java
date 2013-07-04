package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.sm.SessionManagement;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Map;
import java.util.Set;

public class IWSNFederationManagerProvider implements Provider<FederationManager<SessionManagement>> {

	private final IWSNFederatorServerConfig config;

	@Inject
	public IWSNFederationManagerProvider(final IWSNFederatorServerConfig config) {
		this.config = config;
	}

	@Override
	public FederationManager<SessionManagement> get() {

		final ImmutableMap.Builder<URI, ImmutableSet<NodeUrnPrefix>> smEndpointUrlPrefixSetBuilder =
				ImmutableMap.builder();

		for (Map.Entry<URI, Set<NodeUrnPrefix>> entry : config.getFederates().entrySet()) {
			smEndpointUrlPrefixSetBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
		}

		final ImmutableMap<URI, ImmutableSet<NodeUrnPrefix>> smEndpointUrlPrefixSet =
				smEndpointUrlPrefixSetBuilder.build();

		final Function<URI, SessionManagement> uriToSessionManagementFunction = new Function<URI, SessionManagement>() {
			@Override
			public SessionManagement apply(@Nullable final URI s) {
				assert s != null;
				return WisebedServiceHelper.getSessionManagementService(s.toString());
			}
		};

		return new FederationManager<SessionManagement>(uriToSessionManagementFunction, smEndpointUrlPrefixSet);
	}
}
