package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import de.uniluebeck.itm.nettyprotocols.NettyProtocolsModule;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBServiceModule;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClientModule;
import de.uniluebeck.itm.tr.iwsn.gateway.plugins.GatewayPluginModule;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiModule;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceModule;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryModule;

import javax.inject.Singleton;
import java.net.URI;

import static com.google.inject.util.Providers.of;

public class GatewayModule extends AbstractModule {

	private final CommonConfig commonConfig;

	private final GatewayConfig gatewayConfig;

	private final DeviceDBConfig deviceDBConfig;

	public GatewayModule(final CommonConfig commonConfig, final GatewayConfig gatewayConfig,
						 final DeviceDBConfig deviceDBConfig) {
		this.commonConfig = commonConfig;
		this.gatewayConfig = gatewayConfig;
		this.deviceDBConfig = deviceDBConfig;
	}

	@Override
	protected void configure() {

		bind(CommonConfig.class).toProvider(of(commonConfig));
		bind(GatewayConfig.class).toProvider(of(gatewayConfig));
		bind(DeviceDBConfig.class).toProvider(of(deviceDBConfig));

		bind(GatewayEventBus.class).to(GatewayEventBusImpl.class).in(Scopes.SINGLETON);
		bind(DeviceManager.class).to(DeviceManagerImpl.class).in(Scopes.SINGLETON);
		bind(EventIdProvider.class).to(IncrementalEventIdProvider.class).in(Scopes.SINGLETON);
		bind(RequestHandler.class).to(RequestHandlerImpl.class).in(Scopes.SINGLETON);
		bind(DeviceAdapterRegistry.class).to(DeviceAdapterRegistryImpl.class).in(Scopes.SINGLETON);

		Multibinder<DeviceAdapterFactory> gatewayDeviceAdapterFactoryMultibinder =
				Multibinder.newSetBinder(binder(), DeviceAdapterFactory.class);

		gatewayDeviceAdapterFactoryMultibinder.addBinding().to(SingleDeviceAdapterFactory.class);

		install(new SchedulerServiceModule());

		install(new NettyClientModule());
		install(new DeviceFactoryModule());

		install(new DeviceDBServiceModule(deviceDBConfig));
		install(new NodeApiModule());
		install(new NettyProtocolsModule());
		install(new ServicePublisherCxfModule());
		install(new GatewayPluginModule());

		final URI smartSantanderEventBrokerUri = gatewayConfig.getSmartSantanderEventBrokerUri();
		if (smartSantanderEventBrokerUri != null && !"".equals(smartSantanderEventBrokerUri.toString())) {
			install(new SmartSantanderEventBrokerObserverModule());
		} else {
			bind(SmartSantanderEventBrokerObserver.class).toProvider(of((SmartSantanderEventBrokerObserver) null));
		}
	}

	@Provides
	@Singleton
	EventBus provideEventBus() {
		return new EventBus("GatewayEventBus");
	}

	@Provides
	@Singleton
	SchedulerService provideSchedulerService(final SchedulerServiceFactory factory) {
		return factory.create(-1, "GatewayScheduler");
	}

	@Provides
	@Singleton
	ServicePublisherConfig provideServicePublisherConfig(final GatewayConfig config) {
		return new ServicePublisherConfig(config.getRestAPIPort(), config.getShiroIni());
	}
}
