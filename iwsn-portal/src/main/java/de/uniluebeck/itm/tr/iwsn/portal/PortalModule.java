package de.uniluebeck.itm.tr.iwsn.portal;

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
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfigServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.devicedb.DeviceDBConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBRestServiceModule;
import de.uniluebeck.itm.tr.devicedb.DeviceDBServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.devicedb.DeviceDBServiceModule;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerModule;
import de.uniluebeck.itm.tr.iwsn.common.SchedulerServiceModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerModule;
import de.uniluebeck.itm.tr.rs.RSServiceConfig;
import de.uniluebeck.itm.tr.rs.RSServiceModule;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAServiceModule;

import java.net.URI;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.inject.util.Providers.of;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class PortalModule extends AbstractModule {

	private final DeviceDBConfig deviceDBConfig;

	private final PortalServerConfig portalServerConfig;

	private final CommonConfig commonConfig;

	private final RSServiceConfig rsServiceConfig;

	private final SNAAServiceConfig snaaServiceConfig;

	private final WiseGuiServiceConfig wiseGuiServiceConfig;

	@Inject
	public PortalModule(final CommonConfig commonConfig,
						final DeviceDBConfig deviceDBConfig,
						final PortalServerConfig portalServerConfig,
						final RSServiceConfig rsServiceConfig,
						final SNAAServiceConfig snaaServiceConfig,
						final WiseGuiServiceConfig wiseGuiServiceConfig) {
		this.commonConfig = commonConfig;
		this.deviceDBConfig = deviceDBConfig;
		this.portalServerConfig = portalServerConfig;
		this.rsServiceConfig = rsServiceConfig;
		this.snaaServiceConfig = snaaServiceConfig;
		this.wiseGuiServiceConfig = wiseGuiServiceConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(of(commonConfig));
		bind(PortalServerConfig.class).toProvider(of(portalServerConfig));
		bind(DeviceDBConfig.class).toProvider(of(deviceDBConfig));
		bind(RSServiceConfig.class).toProvider(of(rsServiceConfig));
		bind(SNAAServiceConfig.class).toProvider(of(snaaServiceConfig));
		bind(WiseGuiServiceConfig.class).toProvider(of(wiseGuiServiceConfig));

		install(new SNAAServiceModule(commonConfig, snaaServiceConfig));
		install(new RSServiceModule(commonConfig, rsServiceConfig));
		install(new DeviceDBServiceModule(deviceDBConfig));

		bind(ServedNodeUrnsProvider.class).to(DeviceDBServedNodeUrnsProvider.class);
		bind(ServedNodeUrnPrefixesProvider.class).to(CommonConfigServedNodeUrnPrefixesProvider.class);

		bind(EventBusFactory.class).to(EventBusFactoryImpl.class);
		bind(PortalEventBus.class).to(PortalEventBusImpl.class).in(Singleton.class);
		bind(ReservationManager.class).to(ReservationManagerImpl.class).in(Singleton.class);

		install(new FactoryModuleBuilder()
				.implement(Reservation.class, ReservationImpl.class)
				.build(ReservationFactory.class)
		);

		install(new NettyServerModule(
				new ThreadFactoryBuilder().setNameFormat("Portal-OverlayBossExecutor %d").build(),
				new ThreadFactoryBuilder().setNameFormat("Portal-OverlayWorkerExecutor %d").build()
		)
		);

		install(new FactoryModuleBuilder()
				.implement(ReservationEventBus.class, ReservationEventBusImpl.class)
				.build(ReservationEventBusFactory.class)
		);

		install(new SchedulerServiceModule());
		install(new ServicePublisherCxfModule());
		install(new ResponseTrackerModule());
		install(new NettyProtocolsModule());

		install(new DeviceDBRestServiceModule());
		install(new SoapApiModule());
		install(new RestApiModule());
		install(new WiseGuiServiceModule());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory, final CommonConfig commonConfig) {
		return factory.create(new ServicePublisherConfig(commonConfig.getPort()));
	}

	@Provides
	TimeLimiter provideTimeLimiter() {
		return new SimpleTimeLimiter(getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool()));
	}

	@Provides
	EndpointManager provideEndpointManager(final CommonConfig commonConfig,
										   final PortalServerConfig portalServerConfig) {
		return new EndpointManager() {

			@Override
			public URI getSnaaEndpointUri() {
				return portalServerConfig.getConfigurationSnaaEndpointUri() != null ?
						portalServerConfig.getConfigurationSnaaEndpointUri() :
						URI.create("http://localhost:" + commonConfig.getPort() + "/soap/v3/snaa");
			}

			@Override
			public URI getRsEndpointUri() {
				return portalServerConfig.getConfigurationRsEndpointUri() != null ?
						portalServerConfig.getConfigurationRsEndpointUri() :
						URI.create("http://localhost:" + commonConfig.getPort() + "/soap/v3/rs");
			}

			@Override
			public URI getSmEndpointUri() {
				return portalServerConfig.getConfigurationSmEndpointUri() != null ?
						portalServerConfig.getConfigurationSmEndpointUri() :
						URI.create("http://localhost:" + commonConfig.getPort() + "/soap/v3/sm");
			}

			@Override
			public URI getWsnEndpointUriBase() {
				return portalServerConfig.getConfigurationWsnEndpointUriBase() != null ?
						portalServerConfig.getConfigurationWsnEndpointUriBase() :
						URI.create("http://localhost:" + commonConfig.getPort() + "/soap/v3/wsn/");
			}
		};
	}
}
