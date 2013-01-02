package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDBModule;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClientModule;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiModule;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryModule;

import java.util.Random;

public class GatewayModule extends AbstractModule {

	private final Random eventIdGenerator = new Random();

	private final GatewayConfig gatewayConfig;

	public GatewayModule(final GatewayConfig gatewayConfig) {
		this.gatewayConfig = gatewayConfig;
	}

	@Override
	protected void configure() {

		bind(GatewayConfig.class).toInstance(gatewayConfig);
		bind(GatewayEventBus.class).to(GatewayEventBusImpl.class).in(Scopes.SINGLETON);
		bind(GatewayDeviceManager.class).to(GatewayDeviceManagerImpl.class).in(Scopes.SINGLETON);
		bind(GatewayEventIdProvider.class).to(GatewayRandomEventIdProvider.class).in(Scopes.SINGLETON);

		install(new NettyClientModule());
		install(new DeviceFactoryModule());
		install(new DeviceConfigDBModule());
		install(new NodeApiModule());
		install(new FactoryModuleBuilder()
				.implement(GatewayDevice.class, GatewayDeviceImpl.class)
				.build(GatewayDeviceFactory.class)
		);
	}

	@Provides
	EventBus provideEventBus() {
		return new EventBus("GatewayEventBus");
	}
}
