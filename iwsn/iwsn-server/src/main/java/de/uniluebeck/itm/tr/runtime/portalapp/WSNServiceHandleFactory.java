package de.uniluebeck.itm.tr.runtime.portalapp;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufDeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppFactory;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.Wiseml;

import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WSNServiceHandleFactory {

	public static WSNServiceHandle create(String secretReservationKey,
										  TestbedRuntime testbedRuntime,
										  String urnPrefix,
										  URL wsnServiceEndpointURL,
										  String wiseMLFilename,
										  String[] reservedNodes,
										  ProtobufDeliveryManager protobufDeliveryManager,
										  ProtobufControllerServer protobufControllerServer) {

		// De-serialize original WiseML and strip out all nodes that are not part of this reservation
		Wiseml wiseML = WiseMLHelper.deserialize(WiseMLHelper.readWiseMLFromFile(wiseMLFilename));
		List<Setup.Node> node = wiseML.getSetup().getNode();
		Iterator<Setup.Node> nodeIterator = node.iterator();

		List<String> reservedNodesList = Arrays.asList(reservedNodes);
		while (nodeIterator.hasNext()) {
			Setup.Node currentNode = nodeIterator.next();
			if (!reservedNodesList.contains(currentNode.getId())) {
				nodeIterator.remove();
			}
		}

		final WSNApp wsnApp = WSNAppFactory.create(testbedRuntime, reservedNodes);

		final WSNServiceImpl wsnService = new WSNServiceImpl(
				urnPrefix,
				wsnServiceEndpointURL,
				wiseML,
				reservedNodes,
				protobufDeliveryManager,
				wsnApp
		);

		return new WSNServiceHandle(
				secretReservationKey,
				wsnServiceEndpointURL,
				wsnService,
				wsnApp,
				protobufControllerServer,
				protobufDeliveryManager
		);
	}

}