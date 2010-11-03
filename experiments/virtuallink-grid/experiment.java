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
String localControllerEndpointURL	= "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":8089/controller";

// Authentication credentials and other relevant information used again and again as method parameters
String urnPrefix 					= "urn:wisebed:uzl1:";
String username						= "testbeduzl1";
String password						= "testbeduzl1";
    
// Endpoint URLs of Authentication (SNAA), Reservation (RS) and Experimentation (iWSN) services
String snaaEndpointURL 				= "http://wisebed.itm.uni-luebeck.de:8890/snaa";
String rsEndpointURL				= "http://wisebed.itm.uni-luebeck.de:8889/rs";
String sessionManagementEndpointURL	= "http://wisebed.itm.uni-luebeck.de:8888/sessions";

// Retrieve Java proxies of the endpoint URLs above
SNAA authenticationSystem 			= SNAAServiceHelper.getSNAAService(snaaEndpointURL);
RS reservationSystem				= RSServiceHelper.getRSService(rsEndpointURL);
SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL); 



//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

//--------------------------------------------------------------------------
// 1st step: authenticate with the system
//--------------------------------------------------------------------------

// build argument types
AuthenticationTriple credentials = new AuthenticationTriple();
credentials.setUrnPrefix(urnPrefix);
credentials.setUsername(username);
credentials.setPassword(password);
List credentialsList = new ArrayList();
credentialsList.add(credentials);

// do the authentication
log.info("Authenticating...");
List secretAuthenticationKeys = authenticationSystem.authenticate(credentialsList);
log.info("Successfully authenticated!");


//--------------------------------------------------------------------------
// 2nd step: reserve some nodes (here: all nodes)
//--------------------------------------------------------------------------

// retrieve the node URNs of all iSense nodes
String serializedWiseML = sessionManagement.getNetwork();
List nodeURNs = WiseMLHelper.getNodeUrns(serializedWiseML, new String[] {"isense"});
log.info("Retrieved the node URNs of all iSense nodes: {}", Arrays.toString(nodeURNs.toArray()));

// create reservation request data to reserve all iSense nodes for 10 minutes
ConfidentialReservationData reservationData = helper.generateConfidentialReservationData(
		nodeURNs,
		new Date(), 10, TimeUnit.MINUTES,
		urnPrefix, username
);

// do the reservation
log.info("Trying to reserve the following nodes: {}", nodeURNs);
List secretReservationKeys = reservationSystem.makeReservation(
		helper.copySnaaToRs(secretAuthenticationKeys),
		reservationData
);
log.info("Successfully reserved nodes: {}", nodeURNs);



//--------------------------------------------------------------------------
// 3rd step: start local controller instance
//--------------------------------------------------------------------------

/*AsyncJobObserver jobs = new AsyncJobObserver(1, TimeUnit.MINUTES); //Timeout for join until unfinished jobs are removed

public class MyController implements Controller{
	public void receive(Message msg) {
		String s = helper.toString(msg);
		log.debug("Received message: " + s);
	}
	public void receiveStatus(RequestStatus status) {
		jobs.receive(status);
	}
}

DelegatingController delegator = new DelegatingController(new MyController());
delegator.publish(localControllerEndpointURL);
log.info("Local controller published on url: {}", localControllerEndpointURL);
*/


//--------------------------------------------------------------------------
// 4th step: get WSN API instance URL --> call getInstance() on Session Management service
//--------------------------------------------------------------------------

log.debug("Using the following parameters for calling getInstance(): {}, {}",
		StringUtils.jaxbMarshal(helper.copyRsToWsn(secretReservationKeys)),
		localControllerEndpointURL
);

String wsnEndpointURL = sessionManagement.getInstance(
		helper.copyRsToWsn(secretReservationKeys),
		localControllerEndpointURL
);

log.info("Got an WSN instance URL, endpoint is: {}", wsnEndpointURL);
WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
WSNAsyncWrapper wsn = WSNAsyncWrapper.of(wsnService, localControllerEndpointURL);


//--------------------------------------------------------------------------
// Steps 5..n: Experiment control using the WSN API
//--------------------------------------------------------------------------
log.info("Starting experiments...");

Thread.sleep(2000);

log.info("Checking if nodes are alive.");

Future areNodesAliveFuture = wsn.areNodesAlive(nodeURNs);
try {
	log.info("{}", areNodesAliveFuture.get());
} catch (Exception e) {
	log.error("" + e, e);
	System.exit(1);
}


//--------------------------------------------------------------------------
// Build data structures to resemble grid topology
//--------------------------------------------------------------------------
List nodesToAlign = new ArrayList(nodeURNs);
java.util.Collections.shuffle(nodesToAlign);

int rows = 6;
int cols = ((int) (Math.floor(nodeURNs.size()/6))) + ((nodeURNs.size() % 6 == 0) ? 0 : 1);
String[][] grid = new String[rows][];

// setup grid
for (int row=0; row<grid.length; row++) {
	grid[row] = new String[cols];
	for (int col=0; col<grid[row].length; col++) {
		if (nodesToAlign.size() > 0) {
			grid[row][col] = nodesToAlign.remove(0);
		}
	} 
}

// print grid
System.out.println();
System.out.println("===== NODE GRID =====");
for (int row=0; row<grid.length; row++) {
	for (int col=0; col<grid[row].length; col++) {
		System.out.print(grid[row][col] + " ");
	}
	System.out.println();
}

// setup neighbor list
ImmutableMultimap.Builder neighborMapBuilder = ImmutableMultimap.builder();
for (int row=0; row<grid.length; row++) {
	for (int col=0; col<grid[row].length; col++) {
		// top
		if (row-1 >= 0) {
			neighborMapBuilder.put(grid[row][col], grid[row-1][col]);
		}
		// left
		if (col-1 >= 0) {
			neighborMapBuilder.put(grid[row][col], grid[row][col-1]);
		}
		// bottom
		if (row+1 < grid.length) {
			neighborMapBuilder.put(grid[row][col], grid[row+1][col]);
		}
		// right
		if (col+1 < grid[row].length) {
			neighborMapBuilder.put(grid[row][col], grid[row][col+1]);
		}
	}
}
ImmutableMultimap neighborMap = neighborMapBuilder.build();

// print neighbor list
System.out.println();
System.out.println("===== NEIGHBOR LIST =====");
for (int nodeIdx=0; nodeIdx<nodeURNs.size(); nodeIdx++) {
	String sourceNodeURN = nodeURNs.get(nodeIdx);
	System.out.print(sourceNodeURN + " => [");
	for (int neighborIdx=0; neighborIdx<neighborMap.get(sourceNodeURN).size(); neighborIdx++) {
		System.out.print(neighborMap.get(sourceNodeURN).get(neighborIdx));
		if (neighborIdx < neighborMap.get(sourceNodeURN).size() -1) {
			System.out.print(", ");
		}
	}
	System.out.println("]");
}

//--------------------------------------------------------------------------
// Setup grid topology on testbed
//--------------------------------------------------------------------------

// disable all physical links
log.info("Disabling all physical links...");
WSNHelper.disableAllPhysicalLinks(wsn, nodeURNs);

log.info("Setting grid virtual links...");
WSNHelper.setVirtualLinks(wsn, neighborMap, wsnEndpointURL);

   	
//--------------------------------------------------------------------------
// Steps 5..n: Shutdown experiment and exit
//--------------------------------------------------------------------------

log.info("Done running experiments. Now freeing WSN service instance...");
sessionManagement.free(helper.copyRsToWsn(secretReservationKeys));

log.info("Freed WSN service instance. Shutting down...");
System.exit(0);
