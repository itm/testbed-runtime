package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Singleton;

public class GatewayModule extends AbstractModule {

	private final GatewayConfig gatewayConfig;

	public GatewayModule(final GatewayConfig gatewayConfig) {
		this.gatewayConfig = gatewayConfig;
	}

	@Override
	protected void configure() {
		bind(GatewayConfig.class).toInstance(gatewayConfig);
		bind(GatewayEventBus.class).to(GatewayEventBusImpl.class).in(Singleton.class);
	}

	@Provides
	EventBus provideEventBus() {
		return new EventBus("GatewayEventBus");
	}
}
