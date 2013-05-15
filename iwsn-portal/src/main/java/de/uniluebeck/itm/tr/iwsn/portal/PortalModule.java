package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.nettyprotocols.NettyProtocolsModule;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.devicedb.DeviceDBServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.devicedb.*;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerModule;
import de.uniluebeck.itm.tr.iwsn.common.SchedulerServiceModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerModule;
import de.uniluebeck.itm.tr.rs.RSConfig;
import de.uniluebeck.itm.tr.rs.RSModule;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.snaa.SNAA;

import java.net.URI;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class PortalModule extends AbstractModule {

	private final PortalConfig config;

	public PortalModule(final PortalConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {

		// either RS and/or SNAA are started remotely as a separate process or embedded into the portal
		if (config.getRsEndpointUri() != null) {
			bind(RS.class).toInstance(WisebedServiceHelper.getRSService(config.getRsEndpointUri().toString()));
		} else {
			bind(ServedNodeUrnsProvider.class).to(DeviceDBServedNodeUrnsProvider.class);
			install(new RSModule(config));
		}

		if (config.getSnaaEndpointUri() != null) {
			bind(SNAA.class).toInstance(WisebedServiceHelper.getSNAAService(config.getSnaaEndpointUri().toString()));
		} else {
			// TODO
			throw new RuntimeException("TODO");
		}

		bind(EventBusFactory.class).to(EventBusFactoryImpl.class);
		bind(PortalConfig.class).toInstance(config);
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
		if (config.getDeviceDBProperties() != null) {
			install(new DeviceDBJpaModule(config.getDeviceDBProperties()));
		} else if (config.getDeviceDBUri() != null) {
			install(new RemoteDeviceDBModule(new RemoteDeviceDBConfig(config.getDeviceDBUri())));
		} else {
			throw new RuntimeException(
					"Either the URI of a remote DeviceDB or the JPA properties file for a local DeviceDB must be set!"
			);
		}
		install(new DeviceDBServiceModule());
		install(new SoapApiModule());
		install(new RestApiModule());
	}

	@Provides
	@Singleton
	DeviceDBService provideDeviceDBService(final DeviceDBServiceFactory factory) {
		return factory.create("/devicedb/rest", "/devicedb");
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
		return factory.create(new ServicePublisherConfig(config.getPort()));
	}

	@Provides
	TimeLimiter provideTimeLimiter() {
		return new SimpleTimeLimiter(getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool()));
	}

	@Provides
	EndpointManager provideEndpointManager() {
		return new EndpointManager() {

			@Override
			public URI getSnaaEndpointUri() {
				return config.getSnaaEndpointUri() != null ?
						config.getSnaaEndpointUri() :
						URI.create("http://localhost:" + config.getPort() + "/soap/v3/snaa");
			}

			@Override
			public URI getRsEndpointUri() {
				return config.getRsEndpointUri() != null ?
						config.getRsEndpointUri() :
						URI.create("http://localhost:" + config.getPort() + "/soap/v3/rs");
			}

			@Override
			public URI getSmEndpointUri() {
				return URI.create("http://localhost:" + config.getPort() + "/soap/v3/sm");
			}

			@Override
			public URI getWsnEndpointUriBase() {
				return URI.create("http://localhost:" + config.getPort() + "/soap/v3/wsn/");
			}
		};
	}
}
