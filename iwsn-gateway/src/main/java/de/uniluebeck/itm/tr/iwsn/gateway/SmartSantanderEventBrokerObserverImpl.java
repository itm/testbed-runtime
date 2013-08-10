package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import eu.smartsantander.eventbroker.client.*;
import eu.smartsantander.eventbroker.client.exceptions.EventBrokerException;
import eu.smartsantander.eventbroker.events.IEventFactory.EventType;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents.AddSensorNode;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents.DelSensorNode;
import eu.smartsantander.eventbroker.events.RegistrationEvents;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.iwsn.gateway.SmartSantanderEventBrokerObserverHelper.convert;

public class SmartSantanderEventBrokerObserverImpl extends AbstractService
		implements SmartSantanderEventBrokerObserver {


	private static final Logger log = LoggerFactory.getLogger(SmartSantanderEventBrokerObserverImpl.class);

	/**
	 * Events this component will listen for on the SmartSantander EventBus
	 */
	public static final EventType[] incomingEvents = {
			EventType.ADD_SENSOR_NODE,
			EventType.DEL_SENSOR_NODE
	};

	private final GatewayConfig gatewayConfig;

	private final IEventReceiverFactory eventReceiverFactory;

	private final IEventPublisherFactory eventPublisherFactory;

	/**
	 * Component used establish a publish/subscribe communication within the Gateway
	 */
	private final GatewayEventBus gatewayEventBus;

	private final Map<NodeUrn, DeviceConfig> nodeUrnDeviceConfigMap = newHashMap();

	private int numPublishedEvents;

	/**
	 * The instance of this interface will establish the connection to the SmartSantander EventBroker
	 */
	private IEventReceiver eventReceiver;

	private IEventPublisher eventPublisher;

	@Inject
	public SmartSantanderEventBrokerObserverImpl(final GatewayConfig gatewayConfig,
												 final IEventReceiverFactory eventReceiverFactory,
												 final IEventPublisherFactory eventPublisherFactory,
												 final GatewayEventBus gatewayEventBus) {
		this.gatewayConfig = gatewayConfig;
		this.eventReceiverFactory = eventReceiverFactory;
		this.eventPublisherFactory = eventPublisherFactory;
		this.gatewayEventBus = gatewayEventBus;
		this.numPublishedEvents = 0;
	}

	@Override
	protected void doStart() {

		log.trace("SmartSantanderEventBrokerObserverImpl.doStart()");

		try {
			connectToEventBroker();
			gatewayEventBus.register(this);
			notifyStarted();
		} catch (EventBrokerException e) {
			log.error("An error occurred while tyring to connect to the SmartSantander EventBroker component {}." +
					"The service could not be started.", e.getMessage(), e
			);
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("SmartSantanderEventBrokerObserverImpl.doStop()");

		try {
			closeConnectionToEventBroker();
			gatewayEventBus.unregister(this);
			notifyStopped();
		} catch (EventBrokerException e) {
			log.error("An error occurred while tyring to disconnect from the SmartSantander EventBroker component: ", e
			);
			notifyFailed(e);
		}
	}

	@Override
	public boolean handleEvent(final EventObject event) {

		try {

			checkNotNull(event);

			switch (event.eventType) {

				case ADD_SENSOR_NODE:
					return onAddSensorNodeEvent(AddSensorNode.parseFrom(event.eventBytes));

				case DEL_SENSOR_NODE:
					return onDelSensorNodeEvent(DelSensorNode.parseFrom(event.eventBytes));

				default:
					log.warn("Events of type {} are not handled.", event.eventType);
					return false;
			}

		} catch (InvalidProtocolBufferException e) {
			log.error("Error while parsing protobuf message received from SmartSantander EventBroker: ", e);
			return false;
		} catch (Exception e) {
			log.error("Error while evaluating event {}: ", event, e);
			return false;
		}
	}

	private boolean onAddSensorNodeEvent(final AddSensorNode addSensorNode) throws InvalidProtocolBufferException {

		if (!gatewayConfig.getSmartSantanderGatewayId().equals(addSensorNode.getParentId())) {
			log.trace("The parent identifier provided by the AddSensorNode event ({}) " +
					"does not match the identifier of this gateway ({})", addSensorNode.getParentId(),
					gatewayConfig.getSmartSantanderGatewayId()
			);
			return false;
		}

		final DeviceConfig deviceConfig = convert(addSensorNode);

		if (deviceConfig == null) {
			log.error("Conversion of AddSensorNode event to DeviceConfig failed: {}", addSensorNode);
			return false;
		}

		synchronized (nodeUrnDeviceConfigMap) {
			nodeUrnDeviceConfigMap.put(deviceConfig.getNodeUrn(), deviceConfig);
		}

		final DeviceFoundEvent deviceFoundEvent = new DeviceFoundEvent(
				deviceConfig.getNodeType(),
				deviceConfig.getNodePort(),
				deviceConfig.getNodeUSBChipID(),
				new MacAddress(deviceConfig.getNodeUrn().getSuffix()),
				deviceConfig
		);

		log.trace("Posting DeviceFoundEvent on the gateway event bus: {}", deviceFoundEvent);
		gatewayEventBus.post(deviceFoundEvent);
		return true;
	}

	private boolean onDelSensorNodeEvent(final DelSensorNode delSensorNode) throws InvalidProtocolBufferException {

		if (!gatewayConfig.getSmartSantanderGatewayId().equals(delSensorNode.getParentId())) {
			log.trace("The parent identifier provided by the DelSensorNode event ({}) " +
					"does not match the identifier of this gateway ({})", delSensorNode.getParentId(),
					gatewayConfig.getSmartSantanderGatewayId()
			);
			return false;
		}

		final DeviceConfig deviceConfig;
		synchronized (nodeUrnDeviceConfigMap) {
			deviceConfig = nodeUrnDeviceConfigMap.get(new NodeUrn(delSensorNode.getNodeId()));
		}

		if (deviceConfig == null) {
			log.warn("Did not find DeviceConfig for node ID \"{}\"! This probably means there was no according "
					+ "AddSensorNode event received before.", delSensorNode.getNodeId()
			);
			return false;
		}

		final DeviceLostEvent deviceLostEvent = new DeviceLostEvent(
				deviceConfig.getNodeType(),
				deviceConfig.getNodePort(),
				deviceConfig.getNodeUSBChipID(),
				new MacAddress(deviceConfig.getNodeUrn().getSuffix()),
				deviceConfig
		);

		log.trace("Posting DeviceLostEvent on the gateway event bus: {}", deviceLostEvent);
		gatewayEventBus.post(deviceLostEvent);
		return true;
	}

	/**
	 * Subscribes to the SmartSantander EventBroker to listen for events indicating attaching or detaching sensor
	 * nodes on the Gateway this instance is running on.<br/>
	 * If such an event is detected, the listener posts a {@link DeviceFoundEvent} and {@link DeviceLostEvent},
	 * respectively
	 * on the {@link GatewayEventBus} to inform other components that devices have been attached or detached,
	 * respectively.
	 *
	 * @throws EventBrokerException
	 * 		Thrown if an exception occurred while listening on the SmartSantander EventBroker component.
	 */
	private void connectToEventBroker() throws EventBrokerException {

		log.trace("Connecting to EventBroker listening for events {} ", Joiner.on(", ").join(incomingEvents));

		eventReceiver = eventReceiverFactory.create(gatewayConfig.getSmartSantanderEventBrokerUri().toString());
		eventReceiver.subscribe(incomingEvents, this, true);

		eventPublisher = eventPublisherFactory.create(gatewayConfig.getSmartSantanderEventBrokerUri().toString());
	}

	private void closeConnectionToEventBroker() throws EventBrokerException {

		log.trace("SmartSantanderEventBrokerObserverImpl.closeConnectionToEventBroker()");

		eventReceiver.unsubscribe(incomingEvents);
		eventReceiver.close();
		eventPublisher.close();
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

			EventObject addSensorNodeReplyEventObject =
					new EventObject(EventType.ADD_SENSOR_NODE_REPLY, reply.toByteArray());
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

			EventObject delSensorNodeReplyEventObject =
					new EventObject(EventType.ADD_SENSOR_NODE_REPLY, reply.toByteArray());

			try {
				eventPublisher.send(delSensorNodeReplyEventObject);
			} catch (EventBrokerException e) {
				log.error("Something went wrong while trying to post an 'DEL_SENSOR_NODE_REPLY' on the EventBroker");
			}
		}

	}

}
