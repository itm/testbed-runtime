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

	// Endpoint URLs of Authentication (SNAA), Reservation (RS) and Experimentation (iWSN) services
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	String nodeUrnsToCheck = System.getProperty("testbed.nodeurns");

	// Retrieve Java proxies of the endpoint URLs above
	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);



//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	AsyncJobObserver jobs = new AsyncJobObserver(10, TimeUnit.SECONDS);

	Controller controller = new Controller() {
		public void receive(List msg) {
			// nothing to do
		}
		public void receiveStatus(List requestStatuses) {
			for (int i=0; i<requestStatuses.size(); i++) {
		    	System.out.println(requestStatuses.get(i));
			}
			jobs.receive(requestStatuses);
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

	// retrieve reserved node URNs from testbed
	List nodeURNs;
	if (nodeUrnsToCheck != null && !"".equals(nodeUrnsToCheck)) {
		nodeURNs = Lists.newArrayList(nodeUrnsToCheck.split(","));
	} else {
		nodeURNs = WiseMLHelper.getNodeUrns(sessionManagement.getNetwork(), new String[]{});
	}
	log.info("Retrieved the following node URNs: {}", nodeURNs);

	log.info("Checking if nodes are alive...");

	String requestId = sessionManagement.areNodesAlive(nodeURNs);

	Job job = new Job("areNodesAlive", requestId, nodeURNs, Job.JobType.areNodesAlive);
	job.addListener(new JobResultListener() {
		public void receiveJobResult(JobResult result) {
			System.out.println(result);
			System.exit(0);
		}
		public void receiveMessage(Message msg) throws IOException {
			System.out.println(msg);
		}
		public void timeout() {
			System.out.println("Timed out!");
			System.exit(1);
		}
	});
	jobs.submit(job, 10, TimeUnit.SECONDS);

