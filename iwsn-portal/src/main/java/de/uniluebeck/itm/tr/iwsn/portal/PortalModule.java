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
import de.uniluebeck.itm.tr.devicedb.*;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerModule;
import de.uniluebeck.itm.tr.iwsn.common.SchedulerServiceModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.SoapApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerModule;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.snaa.SNAA;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static com.google.common.base.Throwables.propagate;

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
		if (portalConfig.deviceDBPropertiesFile != null) {
			install(new DeviceDBJpaModule(readProperties(portalConfig.deviceDBPropertiesFile)));
		} else if (portalConfig.deviceDBUri != null) {
			install(new RemoteDeviceDBModule(new RemoteDeviceDBConfig(portalConfig.deviceDBUri)));
		} else {
			throw new RuntimeException(
					"Either the URI of a remote DeviceDB or the JPA properties file for a local DeviceDB must be set!"
			);
		}
		install(new DeviceDBServiceModule());
		install(new SoapApiModule());
	}

	private Properties readProperties(final File propertiesFile) {
		try {
			final Properties properties = new Properties();
			properties.load(new FileReader(propertiesFile));
			return properties;
		} catch (IOException e) {
			throw propagate(e);
		}
	}

	@Provides
	@Singleton
	DeviceDBService provideDeviceDBService(final DeviceDBServiceFactory factory) {
		return factory.create("/devicedb/rest");
	}

	@Provides
	@Singleton
	EventBus provideEventBus() {
		return new EventBus("PortalEventBus");
	}

	@Provides
	@Singleton
	RS provideRS() {
		return WisebedServiceHelper.getRSService(portalConfig.rsEndpointUrl.toString());
	}

	@Provides
	@Singleton
	SNAA provideSNAA(){
		return WisebedServiceHelper.getSNAAService(portalConfig.snaaEndpointUrl.toString());
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
