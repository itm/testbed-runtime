import java.util.*;
import java.nio.*;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;

import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.testbed.api.rs.v1.PublicReservationData;
import eu.wisebed.testbed.api.rs.v1.RS;

import eu.wisebed.testbed.api.snaa.v1.SNAA;
import eu.wisebed.testbed.api.snaa.v1.AuthenticationTriple;
import eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;

import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.api.controller.*;
import eu.wisebed.api.common.*;
import eu.wisebed.api.sm.*;
import eu.wisebed.api.wsn.*;

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
	String messageToSend = System.getProperty("testbed.message");
	String selectedNodeUrns = System.getProperty("testbed.nodeurns");

	String pccHost = System.getProperty("testbed.protobuf.hostname");
	Integer pccPort = Integer.parseInt(System.getProperty("testbed.protobuf.port"));

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	
	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);


//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	String wsnEndpointURL = null;
	try {
		wsnEndpointURL = sessionManagement.getInstance(
				helper.parseSecretReservationKeys(secretReservationKeys),
				"NONE"
		);
	} catch (UnknownReservationIdException_Exception e) {
		log.warn("There was not reservation found with the given secret reservation key. Exiting.");
		System.exit(1);
	}

	log.info("Got a WSN instance URL, endpoint is: {}", wsnEndpointURL);
	WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
	final WSNAsyncWrapper wsn = WSNAsyncWrapper.of(wsnService);

	ProtobufControllerClient pcc = ProtobufControllerClient.create(pccHost, pccPort, helper.parseSecretReservationKeys(secretReservationKeys));
	pcc.addListener(new ProtobufControllerClientListener() {
		public void receive(List msg) {
			// nothing to do
		}
		public void receiveStatus(List requestStatuses) {
			wsn.receive(requestStatuses);
		}
		public void receiveNotification(List msgs) {
			for (int i=0; i<msgs.size(); i++) {
				log.info(msgs.get(i));
			}
		}
		public void experimentEnded() {
			log.info("Experiment ended");
			System.exit(0);
		}
		public void onConnectionEstablished() {
			log.debug("Connection established.");
		}
		public void onConnectionClosed() {
			log.debug("Connection closed.");
		}
	});
	pcc.connect();

	// retrieve reserved node URNs from testbed
	List nodeURNs;
	if (selectedNodeUrns != null && !"".equals(selectedNodeUrns)) {
		nodeURNs = Lists.newArrayList(selectedNodeUrns.split(","));
	} else {
		nodeURNs = WiseMLHelper.getNodeUrns(wsn.getNetwork().get(), new String[]{});
	}
	log.info("Selected the following node URNs: {}", nodeURNs);

	
	// Constructing UART Message from Input String (Delimited by ",")
	// Supported Prefixes are "0x" and "0b", otherwise Base_10 (DEZ) is assumed	
	String[] splitMessage = messageToSend.split(",");
	byte[] messageToSendBytes = new byte[splitMessage.length];
	String messageForOutputInLog = "";
	for (int i=0;i<splitMessage.length;i++) {
		int type = 10;
		if (splitMessage[i].startsWith("0x")) {
			type = 16;
			splitMessage[i]=splitMessage[i].replace("0x","");
		} else if (splitMessage[i].startsWith("0b")) {
			type = 2;
			splitMessage[i]=splitMessage[i].replace("0b","");
		}
		BigInteger b = new BigInteger(splitMessage[i], type);
		messageToSendBytes[i] = (byte) b.intValue() ;
		messageForOutputInLog = messageForOutputInLog + b.intValue() +" ";
	}
	
	log.info("Sending Message [ "+messageForOutputInLog+"] to nodes...");
	
	// Constructing the Message
	Message binaryMessage = new Message();
	binaryMessage.setBinaryData(messageToSendBytes);
	
	GregorianCalendar c = new GregorianCalendar();
	c.setTimeInMillis(System.currentTimeMillis());

	binaryMessage.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
	binaryMessage.setSourceNodeId("urn:wisebed:uzl1:0xFFFF");
	
	Future sendFuture = wsn.send(nodeURNs, binaryMessage, 3, TimeUnit.MINUTES);
	JobResult sendJobResult = sendFuture.get();
	log.info("{}", sendJobResult);

	
	log.info("Closing connection...");
	pcc.disconnect();
	
	log.info("Shutting down...");
	System.exit(0);
