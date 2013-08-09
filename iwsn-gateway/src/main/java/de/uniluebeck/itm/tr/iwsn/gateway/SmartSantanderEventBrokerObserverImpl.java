package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import eu.smartsantander.eventbroker.client.EventObject;
import eu.smartsantander.eventbroker.client.IEventPublisher;
import eu.smartsantander.eventbroker.client.IEventReceiver;
import eu.smartsantander.eventbroker.client.exceptions.EventBrokerException;
import eu.smartsantander.eventbroker.events.IEventFactory.EventType;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents.AddSensorNode;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents.DelSensorNode;
import eu.smartsantander.eventbroker.events.RegistrationEvents;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.iwsn.gateway.SmartSantanderEventBrokerObserverHelper.convert;

public class SmartSantanderEventBrokerObserverImpl extends AbstractService implements SmartSantanderEventBrokerObserver {


	private static final Logger log = LoggerFactory.getLogger(SmartSantanderEventBrokerObserverImpl.class);

	private int numPublishedEvents;

	/**
	 * The identifier of the Gateway which hosts this instance.
	 */
	private final String gatewayID;

	/**
	 * The instance of this interface will establish the connection to the SmartSantander EventBroker
	 */
	private final IEventReceiver eventReceiver;

	private final IEventPublisher eventPublisher;

	/**
	 * Component used establish a publish/subscribe communication within the Gateway
	 */
	private final GatewayEventBus gatewayEventBus;

	/**
	 * Events this component will listen for on the SmartSantander EventBus
	 */
	public static final EventType[] incomingEvents = {EventType.ADD_SENSOR_NODE,
			EventType.DEL_SENSOR_NODE};

	@Inject
	public SmartSantanderEventBrokerObserverImpl(@Named(GatewayConfig.SMARTSANTANDER_GATEWAY_ID) final String gatewayID,
	                                             final IEventReceiver eventReceiver,
	                                             final IEventPublisher eventPublisher,
	                                             final GatewayEventBus gatewayEventBus) {
		this.gatewayID = gatewayID;
		this.eventReceiver = eventReceiver;
		this.eventPublisher = eventPublisher;
		this.gatewayEventBus = gatewayEventBus;
		numPublishedEvents = 0;
	}

	@Override
	protected void doStart() {

		try {
			connectToEventBroker();
			gatewayEventBus.register(this);
			notifyStarted();
		} catch (EventBrokerException e) {
			log.error("An error occurred while tyring to connect to the SmartSantander EventBroker component {}." +
					"The service could not be started.",e.getMessage(), e);
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			eventReceiver.unsubscribe(incomingEvents);
			gatewayEventBus.unregister(this);
			notifyStopped();
		} catch (EventBrokerException e) {
			log.error("An error occurred while tyring to disconnect from the SmartSantander EventBroker component: ",e);
			notifyFailed(e);
		}
	}

	@Override
	public boolean handleEvent(final EventObject event) {
		try {
			checkNotNull(event);

			switch (event.eventType) {

				case ADD_SENSOR_NODE:
					final AddSensorNode addSensorNode = AddSensorNode.parseFrom(event.eventBytes);
					if (gatewayID.equals(addSensorNode.getParentId())) {
						DeviceConfig deviceConfig = convert(addSensorNode);
						if (deviceConfig != null){
							gatewayEventBus.post(new DeviceFoundEvent(null, deviceConfig));
							log.trace("A DeviceConnectedEvent was posted on the gateway event bus.");
							return true;
						}
						log.warn("No device configuration was found matching the information in the AddSensorNode event {} ", addSensorNode);
						return false;

					}
					log.trace("The parent identifier provided by the AddSensorNode event ({}) " +
							"does not match the identifier of this gateway ({})", addSensorNode.getParentId(), gatewayID);
					return false;


				case DEL_SENSOR_NODE:
					final DelSensorNode delSensorNode = DelSensorNode.parseFrom(event.eventBytes);
					if (gatewayID.equals(delSensorNode.getParentId())) {
						DeviceLostEvent deviceLostEvent = new DeviceLostEvent(
								null,
								new NodeUrn(delSensorNode.getNodeId()));
						gatewayEventBus.post(deviceLostEvent);
						log.trace("A DeviceLostEvent was posted on the gateway event bus.");
						return true;
					}
					log.trace("The parent identifier provided by the DelSensorNode event ({}) " +
							"does not match the identifier of this gateway ({})", delSensorNode.getParentId(), gatewayID);
					return false;

				default:
					log.warn("Events of type {} ar not handled.", event.eventType);
					return false;
			}

		} catch (InvalidProtocolBufferException e) {
			log.error("Error while parsing protobuf message received from SmartSantander EventBroker: ", e);
			return false;
		} catch (Exception e){
			log.error("Error while evaluating event {}: ", event , e);
			return false;
		}
	}

	/**
	 * Subscribes to the SmartSantander EventBroker to listen for events indicating attaching or detaching sensor
	 * nodes on the Gateway this instance is running on.<br/>
	 * If such an event is detected, the listener posts a {@link DeviceFoundEvent} and {@link DeviceLostEvent}, respectively
	 * on the {@link GatewayEventBus} to inform other components that devices have been attached or detached, respectively.
	 *
	 * @throws EventBrokerException Thrown if an exception occurred while listening on the SmartSantander EventBroker component.
	 */
	private void connectToEventBroker() throws EventBrokerException {

		log.trace("Connecting to EventBroker listening for events {} ", Joiner.on(", ").join(incomingEvents));

		eventReceiver.subscribe(incomingEvents, this, true);
	}

	@Override
	@Subscribe
	public void onDevicesAttachedEvent(final DevicesConnectedEvent event) {
		log.trace("SmartSantanderEventBrokerObserverImpl.onDevicesConnectedEvent({})", event);

		for (NodeUrn nodeUrn : event.getNodeUrns()) {

			RegistrationEvents.EventHeader header = RegistrationEvents.EventHeader.newBuilder()
					.setEventTypeId(EventType.ADD_SENSOR_NODE_REPLY.id())
					.setRequestId(numPublishedEvents++)
					.build();

			NodeOperationsEvents.AddSensorNodeReply reply = NodeOperationsEvents.AddSensorNodeReply.newBuilder()
					.setHeader(header)
					.setIotNodeType(RegistrationEvents.RegRequestHeader.IoTNodeType.SENSOR_NODE)
					.setNodeId(nodeUrn.toString())
					.setResponse(true)
					.build();

			EventObject addSensorNodeReplyEventObject = new EventObject(EventType.ADD_SENSOR_NODE_REPLY,reply.toByteArray());
			try {
				eventPublisher.send(addSensorNodeReplyEventObject);
			} catch (EventBrokerException e) {
				log.error("Something went wrong while trying to post an 'ADD_SENSOR_NODE_REPLY' on the EventBroker");
			}
		}

	}

	@Override
	@Subscribe
	public void onDevicesDetachedEvent(final DevicesDisconnectedEvent event) {
		log.trace("SmartSantanderEventBrokerObserverImpl.onDevicesDisconnectedEvent({})", event);

		for (NodeUrn nodeUrn : event.getNodeUrns()) {

			RegistrationEvents.EventHeader header = RegistrationEvents.EventHeader.newBuilder()
					.setEventTypeId(EventType.DEL_SENSOR_NODE_REPLY.id())
					.setRequestId(numPublishedEvents++)
					.build();

			NodeOperationsEvents.DelSensorNodeReply reply = NodeOperationsEvents.DelSensorNodeReply.newBuilder()
					.setHeader(header)
					.setIotNodeType(RegistrationEvents.RegRequestHeader.IoTNodeType.SENSOR_NODE)
					.setNodeId(nodeUrn.toString())
					.setResponse(true)
					.build();

			EventObject delSensorNodeReplyEventObject = new EventObject(EventType.ADD_SENSOR_NODE_REPLY,reply.toByteArray());

			try {
				eventPublisher.send(delSensorNodeReplyEventObject);
			} catch (EventBrokerException e) {
				log.error("Something went wrong while trying to post an 'DEL_SENSOR_NODE_REPLY' on the EventBroker");
			}
		}

	}

}
