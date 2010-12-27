package com.coalesenses.serial.connector;

import com.coalesenses.otap.core.connector.DeviceConnector;
import com.coalesenses.otap.core.seraerial.SerAerialPacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 19.11.2010
 * Time: 13:32:47
 * To change this template use File | Settings | File Templates.
 */
public class SerialConnector extends DeviceConnector {

    private static final int MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

    private static final int MESSAGE_TYPE_WISELIB_UPSTREAM = 105;

    private static final byte NODE_OUTPUT_TEXT = 50;

    private static final byte NODE_OUTPUT_BYTE = 51;

    private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

    private static Logger log = LoggerFactory.getLogger(SerialConnector.class);

    protected iSenseDevice USBDevice;

    private iSenseDeviceListener deviceListener = new iSenseDeviceListenerAdapter() {

        @Override
        public void receivePacket(final MessagePacket p) {

            log.trace("Received message packet: {}", p);

//            boolean isSerAerialPacket = p.getType() == PacketTypes.SERAERIAL && p.getContent().length > 0;
//            SerAerialPacket seraerialPacket = SerialConnector.super.receivePacket(p);
//            if (seraerialPacket.getPacketType()
//                    == com.coalesenses.otap.core.seraerial.SerAerialPacket.PacketType.Packet) {
//                notifyListeners(seraerialPacket);
//            }


            boolean isWiselibUpstream = p.getType() == MESSAGE_TYPE_WISELIB_UPSTREAM;
            boolean isByteTextOrVLink =
                    (p.getContent()[0] & 0xFF) == NODE_OUTPUT_BYTE || (p.getContent()[0] & 0xFF) == NODE_OUTPUT_TEXT
                            || (p.getContent()[0] & 0xFF) == NODE_OUTPUT_VIRTUAL_LINK;

            boolean isWiselibReply = isWiselibUpstream && !isByteTextOrVLink;

            if (isWiselibReply)

            {
                if (log.isDebugEnabled()) {
                    //log.debug("{} => Received WISELIB_UPSTREAM packet with content: {}", nodeUrn, p);
                }
                //nodeApiDeviceAdapter.receiveFromNode(ByteBuffer.wrap(p.getContent()));
            } else

            {
                SerialConnector.this.receivePacket(p);
            }
        }


    };

    public SerialConnector(iSenseDevice USBDevice) {
        this.USBDevice = USBDevice;
        this.USBDevice.registerListener(deviceListener);
    }

    @Override
    public void handleConfirm(SerAerialPacket p) {
    }


    @Override
    public void shutdown() {
        USBDevice.shutdown();
        stop = true;
    }

    @Override
    public boolean sendPacket(SerAerialPacket p) {
        if (p instanceof com.coalesenses.otap.core.seraerial.SerialRoutingPacket) {
            return send(PacketTypes.ISENSE_ISHELL_INTERPRETER & 0xFF, p.toByteArray());

        } else {
            return send(PacketTypes.SERAERIAL & 0xFF, p.toByteArray());

        }
    }

    @Override
    public boolean send(int type, byte[] b) {
        if (b == null || type > 0xFF) {
            log.warn("Skipping empty packet or type > 0xFF.");
            return false;
        }

        try {
            MessagePacket p = new MessagePacket(type & 0xFF, b);
            USBDevice.send(p);
            return true;
        } catch (Exception e) {
            log.warn("Unable to send packet:" + e, e);
            return false;
        }
    }


    public void setUSBDevice(iSenseDevice USBDevice) {
        this.USBDevice = USBDevice;
    }
}
