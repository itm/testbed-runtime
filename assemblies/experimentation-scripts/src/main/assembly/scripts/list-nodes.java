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

import com.google.common.base.*;
import com.google.common.collect.*;

String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
String nodeTypesString = System.getProperty("testbed.nodetypes");
Iterable nodeTypes = Splitter.on(",").trimResults().omitEmptyStrings().split(nodeTypesString);

SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);
List nodes = WiseMLHelper.getNodes(sessionManagement.getNetwork(), nodeTypes);

System.out.println(Joiner.on("\n").join(WiseMLHelper.FUNCTION_NODE_LIST_TO_STRING_LIST.apply(nodes)));
