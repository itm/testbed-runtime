package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import eu.smartsantander.testbed.eventbroker.AMQEventPublisher;
import eu.smartsantander.testbed.eventbroker.AMQEventReceiver;
import eu.smartsantander.testbed.eventbroker.IEventPublisher;
import eu.smartsantander.testbed.eventbroker.IEventReceiver;
import eu.smartsantander.testbed.eventbroker.exceptions.EventBrokerException;

/**
 * Instances of this class configure the parameters for connecting Testbed Runtime to the SmartSantander
 * EventBroker component to listen for events on the EventBroker indicating that a device was attached or
 * detached, respectively.
 */
public class SmartSantanderEventBrokerObserverModule extends AbstractModule {

	private final GatewayConfig gatewayConfig;

	public SmartSantanderEventBrokerObserverModule(final GatewayConfig gatewayConfig) {
		this.gatewayConfig = gatewayConfig;
	}

	@Override
	protected void configure() {

		requireBinding(GatewayEventBus.class);

		bindConstant().annotatedWith(Names.named(GatewayConfig.SMARTSANTANDER_EVENT_BROKER_URI))
				.to(gatewayConfig.getSmartSantanderEventBrokerUri().toString());
		bindConstant().annotatedWith(Names.named(GatewayConfig.SMARTSANTANDER_GATEWAY_ID))
				.to(gatewayConfig.getSmartSantanderGatewayId());

		bind(SmartSantanderEventBrokerObserver.class)
				.to(SmartSantanderEventBrokerObserverImpl.class)
				.in(Scopes.SINGLETON);
	}

	@Provides
	@Singleton
	public IEventReceiver provideIEventReceiver() throws EventBrokerException {
		return new AMQEventReceiver(gatewayConfig.getSmartSantanderEventBrokerUri().toString());
	}

	@Provides
	@Singleton
	public IEventPublisher provideIEventPublisher() throws EventBrokerException {
		return new AMQEventPublisher(gatewayConfig.getSmartSantanderEventBrokerUri().toString());
	}
}
