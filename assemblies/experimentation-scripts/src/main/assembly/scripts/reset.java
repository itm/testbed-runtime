import java.util.*;
import java.nio.*;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.api.rs.PublicReservationData;
import eu.wisebed.api.rs.RS;

import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SecretAuthenticationKey;
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



//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	// Endpoint URL of local controller instance, the testbed will use this URL to send us node outputs
	String localControllerEndpointURL	= "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":8090/controller";
	String secretReservationKeys 		= System.getProperty("testbed.secretreservationkeys");

	// Endpoint URLs of Authentication (SNAA), Reservation (RS) and Experimentation (iWSN) services
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	String nodeUrnsToReset 				= System.getProperty("testbed.nodeurns");

	// Retrieve Java proxies of the endpoint URLs above
	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);



//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	Controller controller = new Controller() {
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
	};

	DelegatingController delegator = new DelegatingController(controller);
	delegator.publish(localControllerEndpointURL);
	log.info("Local controller published on url: {}", localControllerEndpointURL);

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

	log.info("Got a WSN instance URL, endpoint is: {}", wsnEndpointURL);
	WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
	final WSNAsyncWrapper wsn = WSNAsyncWrapper.of(wsnService);

	// retrieve reserved node URNs from testbed
	List nodeURNs;
	if (nodeUrnsToReset != null && !"".equals(nodeUrnsToReset)) {
		nodeURNs = Lists.newArrayList(nodeUrnsToReset.split(","));
	} else {
		nodeURNs = WiseMLHelper.getNodeUrns(wsn.getNetwork().get(), new String[]{});
	}
	log.info("Retrieved the following node URNs: {}", nodeURNs);

	log.info("Resetting nodes...");

	Future resetFuture = wsn.resetNodes(nodeURNs, 10, TimeUnit.SECONDS);
	JobResult resetJobResult = resetFuture.get();
	log.info("{}", resetJobResult);
	if (resetJobResult.getSuccessPercent() < 100) {
		System.out.println("Not all nodes could be reset. Exiting");
		System.exit(1);
	}

	log.info("Done. Shutting down...");
	System.exit(0);
