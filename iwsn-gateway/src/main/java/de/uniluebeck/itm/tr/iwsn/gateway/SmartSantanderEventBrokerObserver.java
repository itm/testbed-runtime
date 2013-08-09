package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import eu.smartsantander.eventbroker.client.IEventListener;

/**
 * Implementations of this interface connect TestbedRuntime to the SmartSantanderEventBroker.<br/>
 * It listens for events on the EventBroker indicating that a device was connected and disconnected, respectively
 * and propagates these events via the {@link GatewayEventBus}.<br/>
 * It also listens for events on the {@link GatewayEventBus} indicating that a device was attached and detached,
 * respectively from the Testbed Runtime framework propagates these events via the EventBroker
 */
public interface SmartSantanderEventBrokerObserver extends IEventListener, Service {

	/**
	 * Listen for events indicating that a new device was logically attached.<br/> In this case, logically attached
	 * means that a corresponding {@link DeviceAdapter} was created and started.
	 *
	 * @param event
	 * 		Event indicating that a new device was logically attached.
	 */
	@Subscribe
	public void onDevicesAttachedEvent(final DevicesConnectedEvent event);

	/**
	 * Consumes events indicating that a {@link DeviceAdapter} was stopped since it does not connect any device to this
	 * gateway (any more).
	 *
	 * @param event
	 * 		Event indicating that a {@link DeviceAdapter} was stopped.
	 */
	@Subscribe
	public void onDevicesDetachedEvent(final DevicesDisconnectedEvent event);


}
