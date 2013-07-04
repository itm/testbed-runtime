package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.federatorutils.FederationManagerServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.federatorutils.FederationManagerServedNodeUrnsProvider;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static com.google.inject.util.Providers.of;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class IWSNFederatorServerModule extends AbstractModule {

	private final IWSNFederatorServerConfig federatorConfig;

	public IWSNFederatorServerModule(final IWSNFederatorServerConfig federatorConfig) {
		this.federatorConfig = federatorConfig;
	}

	@Override
	protected void configure() {

		install(new ServicePublisherCxfModule());

		bind(IWSNFederatorServerConfig.class).toProvider(of(federatorConfig));
		bind(ServedNodeUrnPrefixesProvider.class).to(FederationManagerServedNodeUrnPrefixesProvider.class);
		bind(ServedNodeUrnsProvider.class).to(FederationManagerServedNodeUrnsProvider.class);

		bind(SessionManagementFederatorService.class).to(SessionManagementFederatorServiceImpl.class).in(
				Scopes.SINGLETON
		);
	}

	@Provides
	RS provideRS(final IWSNFederatorServerConfig config) {
		return WisebedServiceHelper.getRSService(config.getFederatorRsEndpointUri().toString());
	}

	@Provides
	@Singleton
	ListeningExecutorService provideListeningExecutorService() {
		return listeningDecorator(newCachedThreadPool(new ThreadFactoryBuilder()
				.setNameFormat("WSN Federator %d")
				.build()
		)
		);
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final IWSNFederatorServerConfig config,
											 final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(config.getPort()));
	}

	@Provides
	@Singleton
	ExecutorService provideExecutorService() {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("SessionManagementFederatorServiceImpl-Thread %d")
				.build();
		return getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool(threadFactory));
	}

	@Provides
	@Singleton
	public FederationManager<SessionManagement> provideFederationManager(final IWSNFederatorServerConfig config) {

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
