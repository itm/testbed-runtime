package com.coalesenses.otap;

import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 10.11.2010
 * Time: 11:25:05
 * To change this template use File | Settings | File Templates.
 */
public class MotapMain {

    private static final Logger log = LoggerFactory.getLogger(MotapMain.class);
    private static List<Long> macs;
    private String nodeUrn;

    private static final Object waitLock = new Object();

    private String nodeType = "isense";

    private String nodeSerialInterface = "/dev/tty.usbserial-000014FA";

    private static iSenseDevice device;

    private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

    private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

    private static final byte NODE_OUTPUT_TEXT = 50;

    private static final byte NODE_OUTPUT_BYTE = 51;

    private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

    private static final byte VIRTUAL_LINK_MESSAGE = 11;

    private iSenseDeviceListener deviceListener = new iSenseDeviceListenerAdapter() {

        @Override
        public void receivePacket(MessagePacket p) {

            log.trace("{} => WSNDeviceAppImpl.receivePacket: {}", nodeUrn, p);

            boolean isWiselibUpstream = p.getType() == MESSAGE_TYPE_WISELIB_UPSTREAM;
            boolean isByteTextOrVLink =
                    (p.getContent()[0] & 0xFF) == NODE_OUTPUT_BYTE || (p.getContent()[0] & 0xFF) == NODE_OUTPUT_TEXT
                            || (p.getContent()[0] & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK;

            boolean isWiselibReply = isWiselibUpstream && !isByteTextOrVLink;

            if (isWiselibReply) {
                if (log.isDebugEnabled()) {
                    log.debug("{} => Received WISELIB_UPSTREAM packet with content: {}", nodeUrn, p);
                }
                //nodeApiDeviceAdapter.receiveFromNode(ByteBuffer.wrap(p.getContent()));
            } else {
                otapPlugin.receivePacket(p);
            }

        }

        @Override
        public void operationCanceled(Operation operation) {

            log.debug("{} => Operation {} canceled.", nodeUrn, operation);


        }

        @Override
        public void operationDone(Operation operation, Object o) {

            log.debug("{} => Operation {} done. Object: {}", new Object[]{nodeUrn, operation, o});


        }

        @Override
        public void operationProgress(Operation operation, float v) {

            log.debug("{} => Operation {} receivedRequestStatus: {}", new Object[]{nodeUrn, operation, v});


        }

    };
    private static OtapPlugin otapPlugin;
    private static final long PRESENCE_DETECT_TIMEOUT = 60000;
    private static boolean force = true;
    private static boolean all;

    private void deliverToMotap(MessagePacket p) {
        otapPlugin.receivePacket(p);
    }


    private void conect() {

        if (nodeSerialInterface == null || "".equals(nodeSerialInterface)) {

            Long macAddress = StringUtils.parseHexOrDecLongFromUrn(nodeUrn);

            if (nodeSerialInterface == null) {
                log.warn(
                        "{} => No serial interface could be detected for {} node. Retrying in 30 seconds.", nodeUrn,
                        nodeType);
                //testbedRuntime.getSchedulerService().schedule(this, 30, TimeUnit.SECONDS);
                return;
            } else {
                log.debug(
                        "{} => Found {} node on serial port {}.", new Object[]{nodeUrn, nodeType, nodeSerialInterface});
            }

        }

        try {

            device = DeviceFactory.create(nodeType, nodeSerialInterface);

        } catch (Exception e) {
            log.warn(
                    "{} => Connection to {} device on serial port {} failed. Reason: {}. Retrying in 30 seconds.",
                    new Object[]{nodeUrn, nodeType, nodeSerialInterface, e.getMessage()});
            //testbedRuntime.getSchedulerService().schedule(this, 30, TimeUnit.SECONDS);
            return;
        }

        log.debug(
                "{} => Successfully connected to {} node on serial port {}",
                new Object[]{nodeUrn, nodeType, nodeSerialInterface});

// attach as listener to device output
        device.registerListener(deviceListener);

// now start listening to messages
//            testbedRuntime.getSingleRequestMultiResponseService()
//                    .addListener(nodeUrn, WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST, srmrsListener);
//            testbedRuntime.getMessageEventService().addListener(messageEventListener);

    }

    public static void main(String[] args) {
        Logging.setLoggingDefaults();
        macs = new Vector<Long>();
        if (!args[0].equals("*")) {
            for (String arg : args) {
                macs.add(StringUtils.parseHexOrDecLongFromUrn(arg));
            }
        } else {
            all = true;
        }
        MotapMain motap = new MotapMain();
        motap.conect();
        otapPlugin = new OtapPlugin();
        otapPlugin.setUSBDevice(device);
        otapPlugin.init();
        otapPlugin.setChannel(18);

        otapPlugin.setOtapKey(null, false);
        otapPlugin.setMultihopSupportState(true);
        otapPlugin.setPresenceDetectState(true);
        long startTime = System.currentTimeMillis();
        List<Long> presentDeviceIds = new Vector<Long>();
        Set<OtapDevice> presentDevices = new HashSet<OtapDevice>();

        while (System.currentTimeMillis() - startTime < PRESENCE_DETECT_TIMEOUT) {
            synchronized (waitLock) {
                try {
                    waitLock.wait(1000);
                    for (Long mac : macs) {
                        for (OtapDevice device : otapPlugin.getPresenceDetect().getDetectedDevices()) {
                            Long id = new Long(device.getId());
                            if (id.longValue() == mac.longValue()) {
                                presentDeviceIds.add(id);
                                presentDevices.add(device);
                                break;
                            }
                        }
                    }
                    if (all) {
                        for (OtapDevice device : otapPlugin.getPresenceDetect().getDetectedDevices()) {
                            Long id = new Long(device.getId());
                            if (!presentDeviceIds.contains(id)) {
                                presentDeviceIds.add(id);
                                presentDevices.add(device);
                            }
                        }
                    }
                    log.info("found the following {} devices: {}", presentDeviceIds.size(), presentDeviceIds);
                    if (presentDeviceIds.size() == macs.size()) {
                        log.info("found all devices");
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        log.info("found the following {} devices: {}", presentDeviceIds.size(), presentDeviceIds);
        if (presentDeviceIds.size() != 0 && (presentDeviceIds.size() == macs.size() || force)) {
            otapPlugin.setPresenceDetectState(false);
            otapPlugin.loadBinProgram(
                    "/Users/maxpagel/Desktop/iSerAerialR1.binmörks");
            log.info("start programming");
            otapPlugin.setParticipatingDeviceList(presentDevices);
            otapPlugin.otapStart();
            //otapPlugin.otapDone();
        } else {
            log.info("not all requested devices have been found. Aborting");
        }

    }

}
