package de.uniluebeck.itm.tr.runtime.portalapp;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufDeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppModule;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.Wiseml;

public class WSNServiceHandleFactory {

	public static WSNServiceHandle create(String secretReservationKey,
										  TestbedRuntime testbedRuntime,
										  String urnPrefix,
										  URL wsnServiceEndpointURL,
										  String wiseMLFilename,
										  ImmutableSet<String> reservedNodes,
										  final ProtobufDeliveryManager protobufDeliveryManager,
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
		final WSNServiceConfig config = new WSNServiceConfig(reservedNodes, wsnServiceEndpointURL, wiseML);
		final WSNPreconditions preconditions = new WSNPreconditions(servedUrnPrefixes, reservedNodes);
		
		final Injector injector = Guice.createInjector(new WSNAppModule());
		final WSNApp wsnApp = injector.getInstance(WSNAppFactory.class).create(testbedRuntime, reservedNodes);
		
		final Injector wsnServiceInjector = Guice.createInjector(new WSNServiceModule());
		final WSNService wsnService = wsnServiceInjector.getInstance(WSNServiceFactory.class).create(config, protobufDeliveryManager, preconditions, wsnApp);
		
		final WSNSoapService wsnSoapService = new WSNSoapService(wsnService, config);

		return new WSNServiceHandle(
				secretReservationKey,
				wsnServiceEndpointURL,
				wsnService,
				wsnSoapService,
				wsnApp,
				protobufControllerServer,
				protobufDeliveryManager
		);
	}

}