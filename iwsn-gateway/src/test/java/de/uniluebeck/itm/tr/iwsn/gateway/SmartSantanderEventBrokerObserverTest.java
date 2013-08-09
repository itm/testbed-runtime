package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.collect.Sets;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesConnectedEvent;
import de.uniluebeck.itm.tr.iwsn.gateway.events.DevicesDisconnectedEvent;
import eu.smartsantander.testbed.eventbroker.EventObject;
import eu.smartsantander.testbed.eventbroker.IEventPublisher;
import eu.smartsantander.testbed.eventbroker.IEventReceiver;
import eu.smartsantander.testbed.eventbroker.exceptions.EventBrokerException;
import eu.smartsantander.testbed.events.IEventFactory;
import eu.smartsantander.testbed.events.NodeOperationsEvents;
import eu.smartsantander.testbed.events.RegistrationEvents;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SmartSantanderEventBrokerObserverHelper.class)
public class SmartSantanderEventBrokerObserverTest {

	@MockitoAnnotations.Mock
	private IEventReceiver eventReceiver;

	@MockitoAnnotations.Mock
	private IEventPublisher eventPublisher;

	@MockitoAnnotations.Mock
	private GatewayEventBus gatewayEventBus;

	private static final String GATEWAY_ID = "testbed-test-gw.smartsantander.eu";

	private SmartSantanderEventBrokerObserver smartSantanderEventBrokerObserver;

	private DeviceConfig deviceConfig;

	@Before
	public void setUp() {
		deviceConfig = new DeviceConfig();
		PowerMockito.mockStatic(SmartSantanderEventBrokerObserverHelper.class);
		PowerMockito
				.when(SmartSantanderEventBrokerObserverHelper.convert(any(NodeOperationsEvents.AddSensorNode.class)))
				.thenReturn(deviceConfig);
		smartSantanderEventBrokerObserver =
				new SmartSantanderEventBrokerObserverImpl(GATEWAY_ID, eventReceiver, eventPublisher, gatewayEventBus);
	}

	@Test
	public void testAddSensorNodeForCorrectGatewayIdentifier() {

		final EventObject event = createAndGetAddSensorNodeEvent(GATEWAY_ID,
				"urn:smartsantander:testbed:70",
				new NodeUrnPrefix("urn:smartsantander:testbed:").toString(),
				"/dev/ttyTestbedRuntime"
		);

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, times(1)).post(any(DeviceConfig.class));

	}

	@Test
	public void testAddSensorNodeForIncorrectIdentifier() {
		final EventObject event = createAndGetAddSensorNodeEvent("abc",
				"urn:smartsantander:testbed:70",
				new NodeUrnPrefix("urn:smartsantander:testbed:").toString(),
				"/dev/ttyTestbedRuntime"
		);

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, never()).post(any(DeviceConfig.class));
	}

	@Test
	public void testAddSensorNodeWithoutDeviceConfig() {

		final EventObject event = createAndGetAddSensorNodeEvent(GATEWAY_ID,
				"urn:smartsantander:testbed:70",
				"",
				"/dev/ttyTestbedRuntime"
		);
		PowerMockito
				.when(SmartSantanderEventBrokerObserverHelper.convert(any(NodeOperationsEvents.AddSensorNode.class)))
				.thenReturn(null);

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, never()).post(any(DeviceConfig.class));

	}

	@Test
	public void testhandleEventWhenEventObjectIsNULL() {

		smartSantanderEventBrokerObserver.handleEvent(null);
		verify(gatewayEventBus, never()).post(any(DeviceConfig.class));

	}

	@Test
	public void testDeleteSensorNodeForCorrectGatewayIdentifier() {
		final EventObject event = createAndGetDelSensorNodeEvent(GATEWAY_ID, "urn:smartsantander:testbed:0815");

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, times(1)).post(any(DeviceConfig.class));
	}

	@Test
	public void testDeleteSensorNodeForUnsetNodeID() {
		final EventObject event = createAndGetDelSensorNodeEvent(GATEWAY_ID, "");

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, never()).post(any(DeviceConfig.class));
	}

	@Test
	public void testDeleteSensorNodeForIncorrectIdentifier() {
		final EventObject event = createAndGetDelSensorNodeEventWithWrongEventType(GATEWAY_ID);

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, never()).post(any(DeviceConfig.class));
	}

	@Test
	public void testDoNotForwardEventIfEventTypeIsWrong() {
		final EventObject event = createAndGetAddSensorNodeEvent("abc",
				"urn:smartsantander:testbed:70",
				new NodeUrnPrefix("urn:smartsantander:testbed:").toString(),
				"/dev/ttyTestbedRuntime"
		);

		smartSantanderEventBrokerObserver.handleEvent(event);
		verify(gatewayEventBus, never()).post(any(DeviceConfig.class));
	}

	@Test
	public void testPostAddSensorNodeReplyOnReceivingADevicesAttachedEvent() {
		DevicesConnectedEvent devicesConnectedEvent = new DevicesConnectedEvent(
				null, Sets.newHashSet(new NodeUrn("urn:smartsantander:testbed:0815"))
		);

		RegistrationEvents.EventHeader header = RegistrationEvents.EventHeader.newBuilder()
				.setEventTypeId(IEventFactory.EventType.ADD_SENSOR_NODE_REPLY.id())
				.setRequestId(0)
				.build();

		NodeOperationsEvents.AddSensorNodeReply reply = NodeOperationsEvents.AddSensorNodeReply.newBuilder()
				.setHeader(header)
				.setIotNodeType(RegistrationEvents.RegRequestHeader.IoTNodeType.SENSOR_NODE)
				.setNodeId("urn:smartsantander:testbed:0815")
				.setResponse(true)
				.build();


		final EventObject eventObject =
				new EventObject(IEventFactory.EventType.ADD_SENSOR_NODE_REPLY, reply.toByteArray());

		smartSantanderEventBrokerObserver.onDevicesAttachedEvent(devicesConnectedEvent);
		try {

			verify(eventPublisher, times(1)).send(any(EventObject.class));
//			verify(eventPublisher, times(1)).send(eq(eventObject));
		} catch (EventBrokerException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testPostDelSensorNodeReplyOnReceivingADevicesDetachedEvent() {

		DevicesDisconnectedEvent devicesDisconnectedEvent = new DevicesDisconnectedEvent(
				null, Sets.newHashSet(new NodeUrn("urn:smartsantander:testbed:0815"))
		);

		RegistrationEvents.EventHeader header = RegistrationEvents.EventHeader.newBuilder()
				.setEventTypeId(IEventFactory.EventType.DEL_SENSOR_NODE_REPLY.id())
				.setRequestId(1)
				.build();

		NodeOperationsEvents.DelSensorNodeReply reply = NodeOperationsEvents.DelSensorNodeReply.newBuilder()
				.setHeader(header)
				.setIotNodeType(RegistrationEvents.RegRequestHeader.IoTNodeType.SENSOR_NODE)
				.setNodeId("urn:smartsantander:testbed:0815")
				.setResponse(true)
				.build();

		EventObject eventObject = new EventObject(IEventFactory.EventType.ADD_SENSOR_NODE_REPLY, reply.toByteArray());


		smartSantanderEventBrokerObserver.onDevicesDetachedEvent(devicesDisconnectedEvent);
		try {

			verify(eventPublisher, times(1)).send(any(EventObject.class));
//			verify(eventPublisher, times(1)).send(eq(eventObject));
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
				.setNodeType("waspmote")
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
				.setNodeId("urn:smartsantander:testbed:0815")
				.build();

		return new EventObject(IEventFactory.EventType.DEL_SENSOR_NODE_REPLY, del_sensor.toByteArray());
	}

	private DevicesConnectedEvent getDevicesAttachedEvent() {
		DevicesConnectedEvent devicesConnectedEvent = new DevicesConnectedEvent(
				null, Sets.newHashSet(new NodeUrn("urn:smartsantander:testbed:0815"))
		);
		return devicesConnectedEvent;
	}

}
