package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import eu.smartsantander.eventbroker.client.IEventPublisherFactory;
import eu.smartsantander.eventbroker.client.IEventPublisherFactoryImpl;
import eu.smartsantander.eventbroker.client.IEventReceiverFactory;
import eu.smartsantander.eventbroker.client.IEventReceiverFactoryImpl;

/**
 * Instances of this class configure the parameters for connecting Testbed Runtime to the SmartSantander
 * EventBroker component to listen for events on the EventBroker indicating that a device was attached or
 * detached, respectively.
 */
public class SmartSantanderEventBrokerObserverModule extends AbstractModule {

	@Override
	protected void configure() {

		requireBinding(GatewayConfig.class);
		requireBinding(GatewayEventBus.class);

		bind(IEventReceiverFactory.class).to(IEventReceiverFactoryImpl.class);
		bind(IEventPublisherFactory.class).to(IEventPublisherFactoryImpl.class);

		bind(SmartSantanderEventBrokerObserver.class)
				.to(SmartSantanderEventBrokerObserverImpl.class)
				.in(Scopes.SINGLETON);
	}
}
