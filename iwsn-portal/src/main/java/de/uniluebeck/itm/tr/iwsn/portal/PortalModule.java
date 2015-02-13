package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.nettyprotocols.NettyProtocolsModule;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.*;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfigServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.devicedb.DeviceDBConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBRestServiceModule;
import de.uniluebeck.itm.tr.devicedb.DeviceDBServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.devicedb.DeviceDBServiceModule;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStoreModule;
import de.uniluebeck.itm.tr.iwsn.portal.externalplugins.ExternalPluginModule;
import de.uniluebeck.itm.tr.iwsn.portal.externalplugins.ExternalPluginServiceConfig;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerModule;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.NodeStatusTracker;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.NodeStatusTrackerImpl;
import de.uniluebeck.itm.tr.iwsn.portal.plugins.PortalPluginModule;
import de.uniluebeck.itm.tr.rs.RSHelperModule;
import de.uniluebeck.itm.tr.rs.RSServiceConfig;
import de.uniluebeck.itm.tr.rs.RSServiceModule;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAServiceModule;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.inject.util.Providers.of;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class PortalModule extends AbstractModule {

	private static final Logger log = LoggerFactory.getLogger(PortalModule.class);

	private final DeviceDBConfig deviceDBConfig;

	private final PortalServerConfig portalServerConfig;

	private final CommonConfig commonConfig;

	private final RSServiceConfig rsServiceConfig;

	private final SNAAServiceConfig snaaServiceConfig;

	private final WiseGuiServiceConfig wiseGuiServiceConfig;

	private final WisemlProviderConfig wisemlProviderConfig;

	private final ExternalPluginServiceConfig externalPluginServiceConfig;

	@Inject
	public PortalModule(final CommonConfig commonConfig,
						final DeviceDBConfig deviceDBConfig,
						final PortalServerConfig portalServerConfig,
						final RSServiceConfig rsServiceConfig,
						final SNAAServiceConfig snaaServiceConfig,
						final WiseGuiServiceConfig wiseGuiServiceConfig,
						final WisemlProviderConfig wisemlProviderConfig,
						final ExternalPluginServiceConfig externalPluginServiceConfig) {
		this.commonConfig = commonConfig;
		this.deviceDBConfig = deviceDBConfig;
		this.portalServerConfig = portalServerConfig;
		this.rsServiceConfig = rsServiceConfig;
		this.snaaServiceConfig = snaaServiceConfig;
		this.wiseGuiServiceConfig = wiseGuiServiceConfig;
		this.wisemlProviderConfig = wisemlProviderConfig;
		this.externalPluginServiceConfig = externalPluginServiceConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(of(commonConfig));
		bind(PortalServerConfig.class).toProvider(of(portalServerConfig));
		bind(DeviceDBConfig.class).toProvider(of(deviceDBConfig));
		bind(RSServiceConfig.class).toProvider(of(rsServiceConfig));
		bind(SNAAServiceConfig.class).toProvider(of(snaaServiceConfig));
		bind(WiseGuiServiceConfig.class).toProvider(of(wiseGuiServiceConfig));
		bind(WisemlProviderConfig.class).toProvider(of(wisemlProviderConfig));
		bind(ExternalPluginServiceConfig.class).toProvider(of(externalPluginServiceConfig));

		bind(ServedNodeUrnsProvider.class).to(DeviceDBServedNodeUrnsProvider.class);
		bind(ServedNodeUrnPrefixesProvider.class).to(CommonConfigServedNodeUrnPrefixesProvider.class);

		bind(EventBusFactory.class).to(EventBusFactoryImpl.class);

		bind(PortalEventBusImpl.class).in(Singleton.class);
		bind(PortalEventBus.class).to(PortalEventBusImpl.class);
		bind(EventBusService.class).to(PortalEventBusImpl.class); // for use in RS, CachedDeviceDB

		bind(ReservationManager.class).to(ReservationManagerImpl.class).in(Singleton.class);
		bind(ReservationCache.class).to(ReservationCacheImpl.class).in(Singleton.class);
		bind(UserRegistrationWebAppService.class).to(UserRegistrationWebAppServiceImpl.class).in(Singleton.class);
		bind(NodeStatusTracker.class).to(NodeStatusTrackerImpl.class).in(Singleton.class);
		bind(EndpointManager.class).to(EndpointManagerImpl.class).in(Singleton.class);

        bind(PortalEventDispatcher.class).to(PortalEventDispatcherImpl.class).in(Singleton.class);

		install(new SNAAServiceModule(commonConfig, snaaServiceConfig));
		install(new RSServiceModule(commonConfig, rsServiceConfig));
		install(new DeviceDBServiceModule(deviceDBConfig));

		install(new FactoryModuleBuilder()
						.implement(Reservation.class, ReservationImpl.class)
						.build(ReservationFactory.class)
		);

		install(new NettyServerModule(
						new ThreadFactoryBuilder().setNameFormat("Portal-OverlayBossExecutor %d").build(),
						new ThreadFactoryBuilder().setNameFormat("Portal-OverlayWorkerExecutor %d").build()
				)
		);

		install(new PortalEventStoreModule());

		install(new FactoryModuleBuilder()
						.implement(ReservationEventBus.class, ReservationEventBusImpl.class)
						.build(ReservationEventBusFactory.class)
		);

		install(new RSHelperModule());
		install(new SchedulerServiceModule());
		install(new ServicePublisherCxfModule());
		install(new ResponseTrackerModule());
		install(new NettyProtocolsModule());

		install(new ApplicationPropertiesModule());

		install(new DeviceDBRestServiceModule());
		install(new SoapApiModule());
		install(new RestApiModule(false));
		install(new WiseGuiServiceModule());

		install(new PortalPluginModule());
		install(new ExternalPluginModule());
	}

	@Provides
	@Singleton
	SchedulerService provideSchedulerService(SchedulerServiceFactory factory) {
		return factory.create(-1, "PortalScheduler");
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory, final CommonConfig commonConfig) {
		return factory.create(new ServicePublisherConfig(commonConfig.getPort()));
	}

	@Provides
	TimeLimiter provideTimeLimiter() {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("TimeLimiter %d").build();
		return new SimpleTimeLimiter(
				getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool(threadFactory))
		);
	}

	@Provides
	Stopwatch provideStopwatch() {
		return Stopwatch.createUnstarted();
	}
}
