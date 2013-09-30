package de.uniluebeck.itm.tr.federator.rs;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpointsFactory;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.rs.RS;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class RSFederatorServiceModule extends AbstractModule {

	@Override
	protected void configure() {

		requireBinding(RSFederatorServiceConfig.class);
		requireBinding(ServicePublisher.class);
		requireBinding(FederatedEndpointsFactory.class);

		bind(RSFederatorService.class).to(RSFederatorServiceImpl.class).in(Scopes.SINGLETON);
	}

	@Provides
	@Named(RSFederatorService.RS_FEDERATOR_EXECUTOR_SERVICE)
	public ExecutorService provideExecutorService() {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("RSFederatorService-Thread %d")
				.build();
		return getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool(threadFactory));
	}

	@Provides
	public FederatedEndpoints<RS> provideRsFederationManager(final RSFederatorServiceConfig config,
															final FederatedEndpointsFactory factory) {

		final Function<URI, RS> uriToRSEndpointFunction = new Function<URI, RS>() {
			@Override
			public RS apply(final URI uri) {
				return WisebedServiceHelper.getRSService(uri.toString());
			}
		};

		final Multimap<URI, NodeUrnPrefix> map = HashMultimap.create();
		for (Map.Entry<URI, Set<NodeUrnPrefix>> entry : config.getFederates().entrySet()) {
			map.putAll(entry.getKey(), entry.getValue());
		}

		return factory.create(uriToRSEndpointFunction, map);
	}
}
