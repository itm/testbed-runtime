package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceFoundEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DeviceLostEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.smartsantander.eventbroker.client.*;
import eu.smartsantander.eventbroker.client.exceptions.EventBrokerException;
import eu.smartsantander.eventbroker.events.IEventFactory;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents;
import eu.smartsantander.eventbroker.events.RegistrationEvents;
import eu.wisebed.api.v3.common.NodeUrn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class SmartSantanderEventBrokerObserverTest {

	static {
		Logging.setLoggingDefaults(LogLevel.ERROR);
	}

	private static final String NODE_URN_STRING = "urn:smartsantander:testbed:70";

	private static final NodeUrn NODE_URN = new NodeUrn(NODE_URN_STRING);

	private static final String NODE_URN_PREFIX_STRING = "urn:smartsantander:testbed:";

	private static final String NODE_PORT = "/dev/ttyTestbedRuntime";

	private static final String NODE_TYPE = "waspmote";

	private static final DeviceConfig NODE_DEVICE_CONFIG = new DeviceConfig(
			NODE_URN,
			NODE_TYPE,
			true,
			NODE_PORT,
			null, null, null, null, null, null, null, null, null, null
	);

	private static final String GATEWAY_ID = "testbed-test-gw.smartsantander.eu";

	private static final String INCORRECT_GATEWAY_ID = "testbed-test-gw2.smartsantander.eu";

	@MockitoAnnotations.Mock
	private IEventReceiver eventReceiver;

	@MockitoAnnotations.Mock
	private IEventPublisher eventPublisher;

	@MockitoAnnotations.Mock
	private GatewayEventBus gatewayEventBus;

	private SmartSantanderEventBrokerObserver smartSantanderEventBrokerObserver;

	@Mock
	private GatewayConfig gatewayConfig;

	@Mock
	private IEventPublisherFactory eventPublisherFactory;

	@Mock
	private IEventReceiverFactory eventReceiverFactory;

	@Mock
	private DeviceDBService deviceDBService;

	@Mock
	private SchedulerService schedulerService;

	@Mock
	private Function<NodeOperationsEvents.AddSensorNode, DeviceConfig> conversionFunction;

	@Before
	public void setUp() throws EventBrokerException {

		when(conversionFunction.apply(any(NodeOperationsEvents.AddSensorNode.class))).thenReturn(NODE_DEVICE_CONFIG);
		when(gatewayConfig.getSmartSantanderEventBrokerUri()).thenReturn(URI.create(""));
		when(gatewayConfig.getSmartSantanderGatewayId()).thenReturn(GATEWAY_ID);
		when(eventPublisherFactory.create(anyString())).thenReturn(eventPublisher);
		when(eventReceiverFactory.create(anyString())).thenReturn(eventReceiver);

		smartSantanderEventBrokerObserver = new SmartSantanderEventBrokerObserverImpl(
				schedulerService,
				gatewayConfig,
				eventReceiverFactory,
				eventPublisherFactory,
				gatewayEventBus,
				deviceDBService,
				conversionFunction
		);

		smartSantanderEventBrokerObserver.startAsync().awaitRunning();
	}

	@Test
	public void testAddSensorNodeForCorrectGatewayId() {

		final EventObject event = createAndGetAddSensorNodeEvent(
				GATEWAY_ID,
				NODE_URN_STRING,
				NODE_URN_PREFIX_STRING,
				NODE_PORT
		);

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus).post(any(DeviceFoundEvent.class));
	}

	@Test
	public void testAddSensorNodeForIncorrectGatewayId() {

		final EventObject event = createAndGetAddSensorNodeEvent(
				INCORRECT_GATEWAY_ID,
				NODE_URN_STRING,
				NODE_URN_PREFIX_STRING,
				NODE_PORT
		);

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, never()).post(any(DeviceFoundEvent.class));
	}

	@Test
	public void testAddSensorNodeWithoutDeviceConfig() {

		final EventObject event = createAndGetAddSensorNodeEvent(
				GATEWAY_ID,
				NODE_URN_STRING,
				"",
				NODE_PORT
		);

		when(conversionFunction.apply(any(NodeOperationsEvents.AddSensorNode.class)))
				.thenThrow(new IllegalArgumentException());

		try {
			smartSantanderEventBrokerObserver.handleEvent(event);
		} catch (IllegalArgumentException expected) {
			verify(gatewayEventBus, never()).post(any(DeviceFoundEvent.class));
		}
	}

	@Test
	public void testHandleEventWhenEventObjectIsNull() {

		try {
			smartSantanderEventBrokerObserver.handleEvent(null);
			fail();
		} catch (NullPointerException expected) {
			verify(gatewayEventBus, never()).post(any(DeviceFoundEvent.class));
		}
	}

	@Test
	public void testDeleteSensorNodeForCorrectGatewayIdentifier() {

		final EventObject delEvent = createAndGetDelSensorNodeEvent(GATEWAY_ID, NODE_URN_STRING);
		final EventObject addEvent = createAndGetAddSensorNodeEvent(GATEWAY_ID, NODE_URN_STRING,
				NODE_URN_PREFIX_STRING, NODE_PORT
		);

		final InOrder inOrder = inOrder(gatewayEventBus);

		smartSantanderEventBrokerObserver.handleEvent(addEvent);
		inOrder.verify(gatewayEventBus).post(any(DeviceFoundEvent.class));

		smartSantanderEventBrokerObserver.handleEvent(delEvent);
		inOrder.verify(gatewayEventBus).post(any(DeviceLostEvent.class));
	}

	@Test
	public void testDeleteSensorNodeForUnsetNodeID() {

		final EventObject event = createAndGetDelSensorNodeEvent(GATEWAY_ID, "");

		try {
			smartSantanderEventBrokerObserver.handleEvent(event);
		} catch (IllegalArgumentException expected) {
			verify(gatewayEventBus, never()).post(anyObject());
		}
	}

	@Test
	public void testDeleteSensorNodeForIncorrectIdentifier() {
		final EventObject event = createAndGetDelSensorNodeEventWithWrongEventType(GATEWAY_ID);

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, never()).post(anyObject());
	}

	@Test
	public void testPostAddSensorNodeReplyOnReceivingADevicesAttachedEvent() {

		DevicesConnectedEvent devicesConnectedEvent = new DevicesConnectedEvent(
				null, Sets.newHashSet(NODE_URN)
		);

		smartSantanderEventBrokerObserver.onDevicesAttachedEvent(devicesConnectedEvent);

		try {
			verify(eventPublisher).send(any(EventObject.class));
		} catch (EventBrokerException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testPostDelSensorNodeReplyOnReceivingADevicesDetachedEvent() {

		DevicesDisconnectedEvent devicesDisconnectedEvent = new DevicesDisconnectedEvent(
				null,
				Sets.newHashSet(NODE_URN)
		);

		smartSantanderEventBrokerObserver.onDevicesDetachedEvent(devicesDisconnectedEvent);

		try {
			verify(eventPublisher).send(any(EventObject.class));
		} catch (EventBrokerException e) {
			e.printStackTrace();
		}
	}


	private static EventObject createAndGetAddSensorNodeEvent(String gatewayID,
															  String nodeID,
															  String urnPrefixString,
															  String nodePort) {

		RegistrationEvents.EventHeader header = RegistrationEvents.EventHeader.newBuilder()
				.setEventTypeId(IEventFactory.EventType.ADD_SENSOR_NODE.id())
				.setRequestId(-1)
				.build();

		RegistrationEvents.Capability temperature = RegistrationEvents.Capability.newBuilder()
				.setName("temperature")
				.setUnit("kelvin")
				.setDatatype("float")
				.setData(false)
				.build();

		RegistrationEvents.Position position = RegistrationEvents.Position.newBuilder()
				.setType(RegistrationEvents.Position.PositionType.INDOOR)
				.setXcoor(24)
				.setYcoor(35)
				.setZcoor(56)
				.build();

		RegistrationEvents.KeyValue digi_mac = RegistrationEvents.KeyValue.newBuilder()
				.setKey("DigiMacAddress")
				.setValue("00:00:00:00:00:00:00:00")
				.build();

		RegistrationEvents.KeyValue exp_mac = RegistrationEvents.KeyValue.newBuilder()
				.setKey("ExperimentMacAddress")
				.setValue("00:00:00:00:00:00:00:00")
				.build();

		RegistrationEvents.KeyValue urn_prefix = RegistrationEvents.KeyValue.newBuilder()
				.setKey("UrnPrefix")
				.setValue(urnPrefixString)
				.build();

		RegistrationEvents.NodeTRConfig tr_config = RegistrationEvents.NodeTRConfig.newBuilder()
				.setNodeType(NODE_TYPE)
				.setNodePort(nodePort)
				.addNodeConfig(digi_mac)
				.addNodeConfig(exp_mac)
				.addNodeConfig(urn_prefix)
				.build();

		NodeOperationsEvents.AddSensorNode add_sensor = NodeOperationsEvents.AddSensorNode.newBuilder()
				.setHeader(header)
				.setIotNodeType(RegistrationEvents.RegRequestHeader.IoTNodeType.SENSOR_NODE)
				.setNodeId(nodeID)
				.setParentId(gatewayID)
				.setNodeTrConfig(tr_config)
				.addSensorCapability(temperature)
				.setPosition(position)
				.build();

		return new EventObject(IEventFactory.EventType.ADD_SENSOR_NODE, add_sensor.toByteArray());
	}

	private EventObject createAndGetDelSensorNodeEvent(String gatewayID, String nodeID) {

		RegistrationEvents.EventHeader header = RegistrationEvents.EventHeader.newBuilder()
				.setEventTypeId(IEventFactory.EventType.DEL_SENSOR_NODE.id())
				.setRequestId(-1)
				.build();

		NodeOperationsEvents.DelSensorNode del_sensor = NodeOperationsEvents.DelSensorNode.newBuilder()
				.setHeader(header)
				.setParentId(gatewayID)
				.setIotNodeType(RegistrationEvents.RegRequestHeader.IoTNodeType.SENSOR_NODE)
				.setNodeId(nodeID)
				.build();

		return new EventObject(IEventFactory.EventType.DEL_SENSOR_NODE, del_sensor.toByteArray());
	}

	private EventObject createAndGetDelSensorNodeEventWithWrongEventType(String gatewayID) {

		RegistrationEvents.EventHeader header = RegistrationEvents.EventHeader.newBuilder()
				.setEventTypeId(IEventFactory.EventType.DEL_SENSOR_NODE.id())
				.setRequestId(-1)
				.build();

		NodeOperationsEvents.DelSensorNode del_sensor = NodeOperationsEvents.DelSensorNode.newBuilder()
				.setHeader(header)
				.setParentId(gatewayID)
				.setIotNodeType(RegistrationEvents.RegRequestHeader.IoTNodeType.SENSOR_NODE)
				.setNodeId(NODE_URN_STRING)
				.build();

		return new EventObject(IEventFactory.EventType.DEL_SENSOR_NODE_REPLY, del_sensor.toByteArray());
	}
}
