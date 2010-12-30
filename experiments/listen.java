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
import eu.wisebed.testbed.api.wsn.v211.*;

import de.uniluebeck.itm.tr.util.*;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;

import de.uniluebeck.itm.wisebed.cmdlineclient.*;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.*;
import java.util.concurrent.Future;
import com.google.common.collect.*;



//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

// Endpoint URL of local controller instance, the testbed will use this URL to send us node outputs
String localControllerEndpointURL	= "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":8091/controller";
String secretReservationKeys = System.getProperty("testbed.secretreservationkeys");

// Endpoint URLs of Authentication (SNAA), Reservation (RS) and Experimentation (iWSN) services
String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");

// Retrieve Java proxies of the endpoint URLs above
SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);  



//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

log.debug("Using the following parameters for calling getInstance(): {}, {}",
		StringUtils.jaxbMarshal(helper.parseSecretReservationKeys(secretReservationKeys)),
		localControllerEndpointURL
);

String wsnEndpointURL = null;
try {
	wsnEndpointURL = sessionManagement.getInstance(
			helper.parseSecretReservationKeys(secretReservationKeys),
			localControllerEndpointURL
	);
} catch (UnknownReservationIdException_Exception e) {
	log.warn("There was not reservation found with the given secret reservation key. Exiting.");
	System.exit(1);
}

log.info("Got an WSN instance URL, endpoint is: {}", wsnEndpointURL);
WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
final WSNAsyncWrapper wsn = WSNAsyncWrapper.of(wsnService);

Controller controller = new Controller() {
	public void receive(Message msg) {
		synchronized(System.out) {
			System.out.print(msg.getTimestamp() + " | " + msg.getSourceNodeId() + " | ");
			if (msg.getTextMessage() != null) {
				String msgString = msg.getTextMessage().getMsg();
				System.out.print(msg.getTextMessage().getMessageLevel() + " | ");
				System.out.println(msgString.endsWith("\n") ? msgString.substring(0, msgString.length()-2) : msgString);
			} else if (msg.getBinaryMessage() != null) {
				System.out.println(StringUtils.toHexString(msg.getBinaryMessage().getBinaryType()) + " | " + StringUtils.toHexString(msg.getBinaryMessage().getBinaryData()));
			}
		}
	}
	public void receiveStatus(RequestStatus status) {
		// nothing to do
	}
};

DelegatingController delegator = new DelegatingController(controller);
delegator.publish(localControllerEndpointURL);
log.info("Local controller published on url: {}", localControllerEndpointURL);

while(true) {
	try {
		System.in.read();
	} catch (Exception e) {
		System.err.println(e);
	}
}
