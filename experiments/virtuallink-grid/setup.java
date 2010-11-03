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
import com.google.common.collect.*;

//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

// Endpoint URLs of Authentication (SNAA), Reservation (RS) and Experimentation (iWSN) services
String sessionManagementEndpointURL	= "http://wisebed.itm.uni-luebeck.de:8888/sessions";

// Retrieve Java proxies of the endpoint URLs above
SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL); 

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

// retrieve the node URNs of all iSense nodes
String serializedWiseML = sessionManagement.getNetwork();
List nodeURNs = WiseMLHelper.getNodeUrns(serializedWiseML, new String[] {"isense"});

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