package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
