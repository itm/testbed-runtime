package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import eu.smartsantander.eventbroker.client.IEventPublisherFactory;
import eu.smartsantander.eventbroker.client.IEventPublisherFactoryImpl;
import eu.smartsantander.eventbroker.client.IEventReceiverFactory;
import eu.smartsantander.eventbroker.client.IEventReceiverFactoryImpl;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents;

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
		bind(new TypeLiteral<Function<NodeOperationsEvents.AddSensorNode, DeviceConfig>>() {})
				.to(SmartSantanderEventBrokerObserverConverter.class);

		bind(SmartSantanderEventBrokerObserver.class)
				.to(SmartSantanderEventBrokerObserverImpl.class)
				.in(Scopes.SINGLETON);
	}
}
