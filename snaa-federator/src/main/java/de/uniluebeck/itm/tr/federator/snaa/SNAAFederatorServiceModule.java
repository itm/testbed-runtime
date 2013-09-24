package de.uniluebeck.itm.tr.federator.snaa;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.PreconditionsFactory;
import de.uniluebeck.itm.tr.federator.utils.FederationManager;
import de.uniluebeck.itm.tr.federator.utils.FederationManagerFactory;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAA;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAModule;
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
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;
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

		switch (snaaFederatorServiceConfig.getSnaaFederatorType()) {

			case API:
				bind(SNAA.class).to(SNAAFederatorServiceImpl.class).in(Scopes.SINGLETON);
				bind(SNAAFederatorService.class).to(SNAAFederatorServiceImpl.class).in(Scopes.SINGLETON);
				break;

			case SHIBBOLETH:
				final SNAAServiceConfig snaaServiceConfig = buildConfig(
						SNAAServiceConfig.class,
						snaaFederatorServiceConfig.getSnaaFederatorProperties()
				);
				install(new ShibbolethSNAAModule(snaaServiceConfig));

				bind(SNAA.class).to(DelegatingSNAAFederatorServiceImpl.class).in(Scopes.SINGLETON);
				bind(SNAAFederatorService.class).to(DelegatingSNAAFederatorServiceImpl.class).in(Scopes.SINGLETON);
				bind(SNAAService.class)
						.annotatedWith(Names.named("authorizationSnaa"))
						.to(SNAAFederatorServiceImpl.class);
				bind(SNAAService.class)
						.annotatedWith(Names.named("authenticationSnaa"))
						.to(ShibbolethSNAA.class);

				break;

			default:
				throw new RuntimeException(
						"Unknown SNAA federator type: " + snaaFederatorServiceConfig.getSnaaFederatorType()
				);
		}

		expose(SNAAFederatorService.class);
		expose(SNAA.class);
	}

	@Provides
	@Named(SNAAFederatorService.SNAA_FEDERATOR_EXECUTOR_SERVICE)
	public ExecutorService provideExecutorService() {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("SNAAFederatorService-Thread %d").build();
		return getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool(threadFactory));
	}

	@Provides
	public FederationManager<SNAA> provideSnaaFederationManager(final SNAAFederatorServiceConfig config,
																final FederationManagerFactory factory) {

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
