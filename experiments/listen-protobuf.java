import java.util.*;
import java.nio.*;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.testbed.api.rs.v1.PublicReservationData;
import eu.wisebed.testbed.api.rs.v1.RS;

import eu.wisebed.testbed.api.snaa.v1.SNAA;
import eu.wisebed.testbed.api.snaa.v1.AuthenticationTriple;
import eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;

import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v22.*;

import de.uniluebeck.itm.tr.util.*;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;

import de.uniluebeck.itm.wisebed.cmdlineclient.*;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.*;
import java.util.concurrent.Future;
import com.google.common.collect.*;

import de.uniluebeck.itm.wisebed.cmdlineclient.protobuf.*;

//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String secretReservationKeys = System.getProperty("testbed.secretreservationkeys");
	String pccHost = System.getProperty("testbed.protobuf.hostname");
	Integer pccPort = Integer.parseInt(System.getProperty("testbed.protobuf.port"));

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	ProtobufControllerClient pcc = ProtobufControllerClient.create(pccHost, pccPort, helper.parseSecretReservationKeys(secretReservationKeys));
	pcc.addListener(new ProtobufControllerClientListener() {
		public void receive(Message msg) {
			synchronized(System.out) {
                System.out.print(msg.getTimestamp() + " | " + msg.getSourceNodeId() + " | ");
                if (msg.getTextMessage() != null) {
                    String msgString = msg.getTextMessage().getMsg();
                    System.out.print(msg.getTextMessage().getMessageLevel() + " | ");
                    System.out.println(msgString.endsWith("\n") ? msgString.substring(0, msgString.length()-2) : msgString);
                } else if (msg.getBinaryMessage() != null) {
                    System.out.println(StringUtils.toHexString(msg.getBinaryMessage().getBinaryType()) + "  | " + StringUtils.toHexString(msg.getBinaryMessage().getBinaryData()));
                }
            }
		}
		public void receiveStatus(RequestStatus requestStatus) {
			// nothing to do
		}
		public void onConnectionEstablished() {
			log.debug("Connection established.");
		}
		public void onConnectionClosed() {
			log.debug("Connection closed.");
		}
	});
	pcc.connect();
	