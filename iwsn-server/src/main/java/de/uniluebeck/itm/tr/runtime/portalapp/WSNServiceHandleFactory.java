package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableSet;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufDeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppFactory;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.Wiseml;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

public class WSNServiceHandleFactory {

	public static WSNServiceHandle create(String secretReservationKey,
										  TestbedRuntime testbedRuntime,
										  String urnPrefix,
										  URL wsnServiceEndpointURL,
										  String wiseMLFilename,
										  ImmutableSet<String> reservedNodes,
										  ProtobufDeliveryManager protobufDeliveryManager,
										  ProtobufControllerServer protobufControllerServer) {

		// De-serialize original WiseML and strip out all nodes that are not part of this reservation
		Wiseml wiseML = WiseMLHelper.deserialize(WiseMLHelper.readWiseMLFromFile(wiseMLFilename));
		List<Setup.Node> node = wiseML.getSetup().getNode();
		Iterator<Setup.Node> nodeIterator = node.iterator();

		while (nodeIterator.hasNext()) {
			Setup.Node currentNode = nodeIterator.next();
			if (!reservedNodes.contains(currentNode.getId())) {
				nodeIterator.remove();
			}
		}

		final ImmutableSet<String> servedUrnPrefixes = ImmutableSet.<String>builder().add(urnPrefix).build();
		final WSNApp wsnApp = WSNAppFactory.create(testbedRuntime, reservedNodes);

		final WSNServiceConfig config = new WSNServiceConfig(reservedNodes, wsnServiceEndpointURL, wiseML);
		final WSNPreconditions preconditions = new WSNPreconditions(servedUrnPrefixes, reservedNodes);
		final WSNServiceImpl wsnService = new WSNServiceImpl(config, protobufDeliveryManager, preconditions, wsnApp);

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