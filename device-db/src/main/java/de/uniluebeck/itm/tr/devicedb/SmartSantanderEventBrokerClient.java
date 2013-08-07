package de.uniluebeck.itm.tr.devicedb;


import com.google.protobuf.InvalidProtocolBufferException;
import eu.smartsantander.testbed.eventbroker.*;
import eu.smartsantander.testbed.eventbroker.exceptions.EventBrokerException;
import eu.smartsantander.testbed.events.IEventFactory;
import eu.smartsantander.testbed.events.IEventFactory.EventType;
import eu.smartsantander.testbed.events.NodeOperationsEvents;
import eu.smartsantander.testbed.events.NodeOperationsEvents.AddGWNode;
import eu.smartsantander.testbed.events.NodeOperationsEvents.AddPSNode;
import eu.smartsantander.testbed.events.NodeOperationsEvents.AddSensorNode;

import eu.wisebed.api.v3.common.NodeUrn;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class SmartSantanderEventBrokerClient implements IEventListener {
    private Logger logger = Logger.getLogger(SmartSantanderEventBrokerClient.class);
    private String eventBrokerURL;
    private ArrayList<EventType> subscriptions;
    private IEventReceiver eventReceiver;
    private IEventPublisher eventPublisher;
    private DeviceDB deviceDB;

    public SmartSantanderEventBrokerClient(String eventBrokerURL, long tstamp, DeviceDB devDB) throws EventBrokerException {

        this.eventBrokerURL = eventBrokerURL;

        subscriptions = new ArrayList<EventType>();

        // -- event subscriptions ----
        subscriptions.add(EventType.ADD_GW_NODE);
        subscriptions.add(EventType.ADD_PS_NODE);
        subscriptions.add(EventType.ADD_SENSOR_NODE);
        subscriptions.add(EventType.DEL_GW_NODE);
        subscriptions.add(EventType.DEL_PS_NODE);
        subscriptions.add(EventType.DEL_SENSOR_NODE);
        eventReceiver = new AMQEventReceiver(eventBrokerURL);
        eventPublisher = new AMQEventPublisher(eventBrokerURL);
        deviceDB=devDB;
    }

    public IEventReceiver getEventReceiver() {
        return eventReceiver;
    }

    public IEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public void start() throws EventBrokerException {
        IEventFactory.EventType[] a = new IEventFactory.EventType[1];
        IEventFactory.EventType[] subs = subscriptions.toArray(a);
        eventReceiver.subscribe(subs, this, false);
        String str = "";
        for (int i = 0; i < subs.length; i++) {
            str += subs[i].eventname() + ", ";
        }
        logger.info("Subscribed to events: " + str + " on broker:" + eventBrokerURL);
    }

    synchronized public boolean handleEvent(EventObject event) {
        try {
            switch (event.eventType) {
                case ADD_PS_NODE:
                    AddPSNode ps = AddPSNode.parseFrom(event.eventBytes);
                    logger.debug("Registration request received from Portal Server " + ps.getNodeId());
                    //Do nothing....
                    break;

                case ADD_GW_NODE:
                    AddGWNode gw = AddGWNode.parseFrom(event.eventBytes);
                    logger.debug("Registration request received from Gateway " + gw.getNodeId());
                    //Do nothing....
                    break;

                case ADD_SENSOR_NODE:
                    AddSensorNode sn = AddSensorNode.parseFrom(event.eventBytes);
                    logger.debug("Registration request received from node " + sn.getNodeId());
                    //Do something....
                    DeviceConfig deviceConfig = DeviceDBRDManager.deviceConfigFromRDResource(sn);
                    break;

                case DEL_GW_NODE:
                    NodeOperationsEvents.DelGWNode gwd = NodeOperationsEvents.DelGWNode.parseFrom(event.eventBytes);
                    logger.debug("Delete request received from Gateway " + gwd.getNodeId());
                    //Do nothing....
                    break;

                case DEL_PS_NODE:
                    NodeOperationsEvents.DelPSNode psd = NodeOperationsEvents.DelPSNode.parseFrom(event.eventBytes);
                    logger.debug("Delete request received from Portal Server " + psd.getNodeId());
                    //Do nothing....
                    break;

                case DEL_SENSOR_NODE:
                    NodeOperationsEvents.DelSensorNode snd = NodeOperationsEvents.DelSensorNode.parseFrom(event.eventBytes);
                    logger.debug("Delete request received from node " + snd.getNodeId());
                    //Do something....
                    this.deviceDB.removeByNodeUrn(new NodeUrn(snd.getNodeId()));
                    break;
                default:
                    logger.error("EventBrokerClient.handleEvent(): Unsupported event type received.");
            }
        } catch (InvalidProtocolBufferException e) {
            logger.error(e.getMessage().toString());
        }
        return true;
    }


}

