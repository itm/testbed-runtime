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
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.CommonConfigServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.devicedb.*;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerModule;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerModule;
import de.uniluebeck.itm.tr.iwsn.portal.plugins.PortalPluginModule;
import de.uniluebeck.itm.tr.rs.RSServiceConfig;
import de.uniluebeck.itm.tr.rs.RSServiceModule;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAServiceModule;

import java.net.URI;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.inject.util.Providers.of;
import static de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig.*;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class PortalModule extends AbstractModule {

	private final DeviceDBConfig deviceDBConfig;

	private final PortalServerConfig portalServerConfig;

	private final CommonConfig commonConfig;

	private final RSServiceConfig rsServiceConfig;

	private final SNAAServiceConfig snaaServiceConfig;

	private final WiseGuiServiceConfig wiseGuiServiceConfig;

	private final WisemlProviderConfig wisemlProviderConfig;

	@Inject
	public PortalModule(final CommonConfig commonConfig,
						final DeviceDBConfig deviceDBConfig,
						final PortalServerConfig portalServerConfig,
						final RSServiceConfig rsServiceConfig,
						final SNAAServiceConfig snaaServiceConfig,
						final WiseGuiServiceConfig wiseGuiServiceConfig,
						final WisemlProviderConfig wisemlProviderConfig) {
		this.commonConfig = commonConfig;
		this.deviceDBConfig = deviceDBConfig;
		this.portalServerConfig = portalServerConfig;
		this.rsServiceConfig = rsServiceConfig;
		this.snaaServiceConfig = snaaServiceConfig;
		this.wiseGuiServiceConfig = wiseGuiServiceConfig;
		this.wisemlProviderConfig = wisemlProviderConfig;
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

		install(new PortalPluginModule());
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
	EndpointManager provideEndpointManager(final PortalServerConfig portalServerConfig) {

		return new EndpointManager() {

			@Override
			public URI getSnaaEndpointUri() {
				return assertNonEmpty(
						portalServerConfig.getConfigurationSnaaEndpointUri(),
						CONFIGURATION_SNAA_ENDPOINT_URI
				);
			}

			@Override
			public URI getRsEndpointUri() {
				return assertNonEmpty(
						portalServerConfig.getConfigurationRsEndpointUri(),
						CONFIGURATION_RS_ENDPOINT_URI
				);
			}

			@Override
			public URI getSmEndpointUri() {
				return assertNonEmpty(
						portalServerConfig.getConfigurationSmEndpointUri(),
						CONFIGURATION_SM_ENDPOINT_URI
				);
			}

			@Override
			public URI getWsnEndpointUriBase() {
				return assertNonEmpty(
						portalServerConfig.getConfigurationWsnEndpointUriBase(),
						CONFIGURATION_WSN_ENDPOINT_URI_BASE
				);
			}

			private URI assertNonEmpty(final URI uri, final String paramName) {
				if (uri == null || uri.toString().isEmpty()) {
					throw new IllegalArgumentException(
							"Configuration parameter " + paramName + " must be set!"
					);
				}
				return uri;
			}
		};
	}
}
