package de.uniluebeck.itm.tr.devicedb;


import com.google.common.base.Joiner;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import eu.smartsantander.eventbroker.client.AMQEventReceiver;
import eu.smartsantander.eventbroker.client.EventObject;
import eu.smartsantander.eventbroker.client.IEventListener;
import eu.smartsantander.eventbroker.client.IEventReceiver;
import eu.smartsantander.eventbroker.client.exceptions.EventBrokerException;
import eu.smartsantander.eventbroker.events.IEventFactory;
import eu.smartsantander.eventbroker.events.NodeOperationsEvents;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.smartsantander.eventbroker.events.IEventFactory.EventType.ADD_SENSOR_NODE;
import static eu.smartsantander.eventbroker.events.IEventFactory.EventType.DEL_SENSOR_NODE;

public class DeviceDBRDEventBrokerClient extends AbstractService implements IEventListener {

	private static final Logger log = LoggerFactory.getLogger(DeviceDBRDEventBrokerClient.class);

	private static final IEventFactory.EventType[] EVENT_SUBSCRIPTIONS = new IEventFactory.EventType[]{
			ADD_SENSOR_NODE,
			DEL_SENSOR_NODE
	};

	private IEventReceiver eventReceiver;

	private final DeviceDBConfig deviceDBConfig;

	private DeviceDBService deviceDB;

	@Inject
	public DeviceDBRDEventBrokerClient(final DeviceDBConfig deviceDBConfig, final DeviceDBService deviceDB)
			throws EventBrokerException {
		this.deviceDBConfig = deviceDBConfig;
		this.deviceDB = deviceDB;
	}

	@Override
	protected void doStart() {

		try {

			eventReceiver = new AMQEventReceiver(deviceDBConfig.getSmartSantanderEventBrokerUri().toString());
			eventReceiver.subscribe(EVENT_SUBSCRIPTIONS, this, false);

			if (log.isInfoEnabled()) {
				log.info("Subscribed to {} events on SmartSantander EventBroker at {} ",
						Joiner.on(",").join(EVENT_SUBSCRIPTIONS),
						deviceDBConfig.getSmartSantanderEventBrokerUri().toString()
				);
			}

			notifyStarted();

		} catch (EventBrokerException e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			eventReceiver.unsubscribe(EVENT_SUBSCRIPTIONS);

			if (log.isInfoEnabled()) {
				log.info("Unsubscribed from {} events on SmartSantander EventBroker at {}",
						Joiner.on(",").join(EVENT_SUBSCRIPTIONS),
						deviceDBConfig.getSmartSantanderEventBrokerUri().toString()
				);
			}

			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	synchronized public boolean handleEvent(EventObject event) {

		try {

			switch (event.eventType) {

				case ADD_SENSOR_NODE:
					final NodeOperationsEvents.AddSensorNode sn = NodeOperationsEvents.AddSensorNode.parseFrom(
							event.eventBytes
					);
					log.info("ADD_SENSOR_NODE event received for node " + sn.getNodeId());
					if (deviceDB.getConfigByNodeUrn(new NodeUrn(sn.getNodeId())) != null) {
						deviceDB.update(DeviceDBRDHelper.deviceConfigFromRDResource(sn));
					} else {
						deviceDB.add(DeviceDBRDHelper.deviceConfigFromRDResource(sn));
					}
					break;

				case DEL_SENSOR_NODE:
					final NodeOperationsEvents.DelSensorNode snd = NodeOperationsEvents.DelSensorNode.parseFrom(
							event.eventBytes
					);
					log.info("DEL_SENSOR_NODE event received for node " + snd.getNodeId());
					this.deviceDB.removeByNodeUrn(new NodeUrn(snd.getNodeId()));
					break;

				default:
					log.error("EventBrokerClient.handleEvent(): Unsupported event type received.");
					break;
			}

		} catch (InvalidProtocolBufferException e) {
			log.error("Exception while parsing protobuf message from EventBroker: ", e);
		}

		return true;
	}


}

