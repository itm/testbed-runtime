package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.nettyprotocols.NettyProtocolsModule;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.ServicePublisherJettyMetroJerseyModule;
import de.uniluebeck.itm.tr.devicedb.RemoteDeviceDBConfig;
import de.uniluebeck.itm.tr.devicedb.RemoteDeviceDBModule;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerModule;
import de.uniluebeck.itm.tr.iwsn.common.SchedulerServiceModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerModule;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.rs.RS;

public class PortalModule extends AbstractModule {

	private final PortalConfig portalConfig;

	public PortalModule(final PortalConfig portalConfig) {
		this.portalConfig = portalConfig;
	}

	@Override
	protected void configure() {

		bind(PortalConfig.class).toInstance(portalConfig);
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
		install(new ServicePublisherJettyMetroJerseyModule());
		install(new ResponseTrackerModule());
		install(new NettyProtocolsModule());
		install(new RemoteDeviceDBModule(new RemoteDeviceDBConfig(portalConfig.deviceDBUri)));
		install(new SoapApiModule());
	}

	@Provides
	EventBus provideEventBus() {
		return new EventBus("PortalEventBus");
	}

	@Provides
	RS provideRS() {
		return WisebedServiceHelper.getRSService(portalConfig.rsEndpointUrl.toString());
	}

	@Provides
	@Singleton
	ServicePublisher provideServicePublisher(final ServicePublisherFactory factory) {
		final ServicePublisherConfig config = new ServicePublisherConfig(
				portalConfig.port,
				this.getClass().getResource("/").toString()
		);
		return factory.create(config);
	}
}
