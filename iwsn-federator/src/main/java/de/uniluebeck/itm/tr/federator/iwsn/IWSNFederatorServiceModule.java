package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.*;
import de.uniluebeck.itm.tr.federator.utils.*;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.portal.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.DeliveryManagerAdapter;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.DeliveryManagerFactory;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class IWSNFederatorServiceModule extends AbstractModule {

	@Override
	protected void configure() {

		requireBinding(ServicePublisher.class);
		requireBinding(IWSNFederatorServiceConfig.class);
		requireBinding(RS.class);
		requireBinding(SNAA.class);

		install(new PreconditionsModule());
		install(new FederationManagerModule());
		install(new FactoryModuleBuilder()
				.implement(FederatedReservation.class, FederatedReservationImpl.class)
				.build(FederatedReservationFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(DeliveryManager.class, DeliveryManagerAdapter.class)
				.build(DeliveryManagerFactory.class)
		);

		bind(EventBusFactory.class).to(EventBusFactoryImpl.class);


		bind(ServedNodeUrnPrefixesProvider.class).to(FederationManagerServedNodeUrnPrefixesProvider.class);
		bind(ServedNodeUrnsProvider.class).to(FederationManagerServedNodeUrnsProvider.class);
		bind(FederatedReservationManager.class).in(Scopes.SINGLETON);
		bind(SessionManagementFederatorService.class)
				.to(SessionManagementFederatorServiceImpl.class)
				.in(Scopes.SINGLETON);

		bind(IWSNFederatorService.class).to(IWSNFederatorServiceImpl.class).in(Scopes.SINGLETON);
		bind(PortalEventBus.class).to(FederatorPortalEventBus.class).in(Scopes.SINGLETON);
		bind(ReservationManager.class).to(FederatedReservationManager.class).in(Scopes.SINGLETON);
		bind(IdProvider.class).to(IncrementalIdProvider.class).in(Scopes.SINGLETON);
		bind(WisemlProvider.class).to(FederatorWiseMLProvider.class);
        bind(ReservationEventDispatcher.class).to(FederatorReservationEventDispatcherImpl.class).in(Scopes.SINGLETON);

		install(new FactoryModuleBuilder()
				.implement(FederatorController.class, FederatorControllerImpl.class)
				.build(FederatorControllerFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(WSNFederatorService.class, WSNFederatorServiceImpl.class)
				.build(WSNFederatorServiceFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(ReservationEventBus.class, FederatedReservationEventBus.class)
				.build(ReservationEventBusFactory.class)
		);

		install(new FactoryModuleBuilder().build(FederatedReservationEventBusAdapterFactory.class));
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
	ExecutorService provideExecutorService() {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("SessionManagementFederatorServiceImpl-Thread %d")
				.build();
		return getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool(threadFactory));
	}

	@Provides
	@Singleton
	public FederatedEndpoints<SessionManagement> provideFederationManager(final IWSNFederatorServiceConfig config,
																		  final FederatedEndpointsFactory factory) {

		final Multimap<URI, NodeUrnPrefix> map = HashMultimap.create();

		for (Map.Entry<URI, Set<NodeUrnPrefix>> entry : config.getFederates().entrySet()) {
			map.putAll(entry.getKey(), entry.getValue());
		}

		final Function<URI, SessionManagement> uriToSessionManagementFunction = new Function<URI, SessionManagement>() {
			@Override
			public SessionManagement apply(@Nullable final URI s) {
				assert s != null;
				return WisebedServiceHelper.getSessionManagementService(s.toString());
			}
		};

		return factory.create(uriToSessionManagementFunction, map);
	}
}
