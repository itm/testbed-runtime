package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDBModule;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClientModule;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiModule;
import de.uniluebeck.itm.wsn.deviceutils.ScheduledExecutorServiceModule;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryModule;

import javax.inject.Singleton;

public class GatewayModule extends AbstractModule {

	private final GatewayConfig gatewayConfig;

	public GatewayModule(final GatewayConfig gatewayConfig) {
		this.gatewayConfig = gatewayConfig;
	}

	@Override
	protected void configure() {

		bind(GatewayConfig.class).toInstance(gatewayConfig);
		bind(GatewayEventBus.class).to(GatewayEventBusImpl.class).in(Scopes.SINGLETON);
		bind(DeviceManager.class).to(DeviceManagerImpl.class).in(Scopes.SINGLETON);
		bind(EventIdProvider.class).to(IncrementalEventIdProvider.class).in(Scopes.SINGLETON);
		bind(RequestHandler.class).to(RequestHandlerImpl.class).in(Scopes.SINGLETON);
		bind(GatewayScheduler.class).to(GatewaySchedulerImpl.class).in(Scopes.SINGLETON);

		Multibinder<DeviceAdapterFactory> gatewayDeviceAdapterFactoryMultibinder =
				Multibinder.newSetBinder(binder(), DeviceAdapterFactory.class);

		gatewayDeviceAdapterFactoryMultibinder.addBinding().to(SingleDeviceAdapterFactory.class);

		install(new NettyClientModule());
		install(new DeviceFactoryModule());
		install(new DeviceConfigDBModule());
		install(new NodeApiModule());
		install(new ScheduledExecutorServiceModule("GatewayScheduler"));
	}

	@Provides
	@Singleton
	EventBus provideEventBus() {
		return new EventBus("GatewayEventBus");
	}
}
