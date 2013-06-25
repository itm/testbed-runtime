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
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
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
import de.uniluebeck.itm.tr.snaa.SNAAConfig;
import de.uniluebeck.itm.tr.snaa.SNAAServiceModule;

import java.net.URI;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.inject.util.Providers.of;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class PortalModule extends AbstractModule {

	private final DeviceDBConfig deviceDBConfig;

	private final PortalConfig portalConfig;

	private final CommonConfig commonConfig;

	private final RSServiceConfig rsServiceConfig;

	private final SNAAConfig snaaConfig;

	@Inject
	public PortalModule(final CommonConfig commonConfig,
						final DeviceDBConfig deviceDBConfig,
						final PortalConfig portalConfig,
						final RSServiceConfig rsServiceConfig,
						final SNAAConfig snaaConfig) {
		this.commonConfig = commonConfig;
		this.deviceDBConfig = deviceDBConfig;
		this.portalConfig = portalConfig;
		this.rsServiceConfig = rsServiceConfig;
		this.snaaConfig = snaaConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(of(commonConfig));
		bind(PortalConfig.class).toProvider(of(portalConfig));
		bind(DeviceDBConfig.class).toProvider(of(deviceDBConfig));
		bind(RSServiceConfig.class).toProvider(of(rsServiceConfig));
		bind(SNAAConfig.class).toProvider(of(snaaConfig));

		install(new SNAAServiceModule(commonConfig, snaaConfig));
		install(new RSServiceModule(commonConfig, rsServiceConfig));
		install(new DeviceDBServiceModule(deviceDBConfig));

		bind(ServedNodeUrnsProvider.class).to(DeviceDBServedNodeUrnsProvider.class);
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
	EndpointManager provideEndpointManager(final CommonConfig commonConfig, final PortalConfig portalConfig) {
		return new EndpointManager() {

			@Override
			public URI getSnaaEndpointUri() {
				return portalConfig.getSnaaEndpointUri() != null ?
						portalConfig.getSnaaEndpointUri() :
						URI.create("http://localhost:" + commonConfig.getPort() + "/soap/v3/snaa");
			}

			@Override
			public URI getRsEndpointUri() {
				return portalConfig.getRsEndpointUri() != null ?
						portalConfig.getRsEndpointUri() :
						URI.create("http://localhost:" + commonConfig.getPort() + "/soap/v3/rs");
			}

			@Override
			public URI getSmEndpointUri() {
				return URI.create("http://localhost:" + commonConfig.getPort() + "/soap/v3/sm");
			}

			@Override
			public URI getWsnEndpointUriBase() {
				return URI.create("http://localhost:" + commonConfig.getPort() + "/soap/v3/wsn/");
			}
		};
	}
}
