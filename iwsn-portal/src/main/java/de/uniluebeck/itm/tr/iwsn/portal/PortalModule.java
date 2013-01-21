package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
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

		install(new NettyServerModule(
				new ThreadFactoryBuilder().setNameFormat("Portal-OverlayBossExecutor %d").build(),
				new ThreadFactoryBuilder().setNameFormat("Portal-OverlayWorkerExecutor %d").build()
		));

		install(new FactoryModuleBuilder()
				.implement(ReservationEventBus.class, ReservationEventBusImpl.class)
				.build(ReservationEventBusFactory.class)
		);
	}

	@Provides
	EventBus provideEventBus() {
		return new EventBus("PortalEventBus");
	}

	@Provides
	RS provideRS() {
		return WisebedServiceHelper.getRSService(portalConfig.rsEndpointUrl.toString());
	}
}
