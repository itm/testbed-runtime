package de.uniluebeck.itm.tr.federator.snaa;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.PreconditionsFactory;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpointsFactory;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class SNAAFederatorServiceModule extends PrivateModule {

	private final SNAAFederatorServiceConfig snaaFederatorServiceConfig;

	public SNAAFederatorServiceModule(final SNAAFederatorServiceConfig snaaFederatorServiceConfig) {
		this.snaaFederatorServiceConfig = checkNotNull(snaaFederatorServiceConfig);
	}

	@Override
	protected void configure() {

		requireBinding(ServicePublisher.class);
		requireBinding(SNAAFederatorServiceConfig.class);
		requireBinding(PreconditionsFactory.class);

		bind(SNAAFederatorServiceImpl.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(SNAAFederatorServiceImpl.class);
		bind(SNAAFederatorService.class).to(SNAAFederatorServiceImpl.class);

		expose(SNAAFederatorService.class);
		expose(SNAA.class);
	}

	@Provides
	@Named(SNAAFederatorService.SNAA_FEDERATOR_EXECUTOR_SERVICE)
	public ExecutorService provideExecutorService() {
		final ThreadFactory threadFactory =
				new ThreadFactoryBuilder().setNameFormat("SNAAFederatorService-Thread %d").build();
		return getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool(threadFactory));
	}

	@Provides
	public FederatedEndpoints<SNAA> provideSnaaFederationManager(final SNAAFederatorServiceConfig config,
																 final FederatedEndpointsFactory factory) {

		final Function<URI, SNAA> uriToRSEndpointFunction = new Function<URI, SNAA>() {
			@Override
			public SNAA apply(final URI uri) {
				return WisebedServiceHelper.getSNAAService(uri.toString());
			}
		};

		final Multimap<URI, NodeUrnPrefix> map = HashMultimap.create();
		for (Map.Entry<URI, Set<NodeUrnPrefix>> entry : config.getFederates().entrySet()) {
			map.putAll(entry.getKey(), entry.getValue());
		}

		return factory.create(uriToRSEndpointFunction, map);
	}
}
