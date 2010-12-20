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



//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

    // Authentication credentials and other relevant information used again and again as method parameters
	String urnPrefix 					= System.getProperty("testbed.urnprefixes");
    String username						= System.getProperty("testbed.usernames");
    String password						= System.getProperty("testbed.passwords");
    
    // Endpoint URLs of Authentication (SNAA), Reservation (RS) and Experimentation (iWSN) services
    String snaaEndpointURL 				= System.getProperty("testbed.snaa.endpointurl");
    String rsEndpointURL				= System.getProperty("testbed.rs.endpointurl");
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");

	Integer duration					= Integer.parseInt(System.getProperty("testbed.duration"));

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
	List iSenseNodeURNs = WiseMLHelper.getNodeUrns(serializedWiseML, new String[] {"isense"});
	log.info("Retrieved the node URNs of all iSense nodes: {}", Arrays.toString(iSenseNodeURNs.toArray()));

	// create reservation request data to reserve all iSense nodes for 10 minutes
	ConfidentialReservationData reservationData = helper.generateConfidentialReservationData(
			iSenseNodeURNs,
			new Date(), duration, TimeUnit.MINUTES,
			urnPrefix, username
	);

	// do the reservation
	log.info("Trying to reserve the following nodes: {}", iSenseNodeURNs);
    try {
    	
    	List secretReservationKeys = reservationSystem.makeReservation(
    			helper.copySnaaToRs(secretAuthenticationKeys),
    			reservationData
    	);
		log.info("Successfully reserved nodes: {}", iSenseNodeURNs);
		log.info("Reservation Key(s): {}", helper.toString(secretReservationKeys));
		
		System.out.println(helper.toString(secretReservationKeys));
		
    } catch (ReservervationConflictExceptionException e) {
    	log.error("" + e);
    	System.exit(1);
    }
