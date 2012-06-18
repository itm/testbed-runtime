package de.uniluebeck.itm.tr.runtime.portalapp;

import static com.google.inject.matcher.Matchers.annotatedWith;

import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.iwsn.IWSNAuthorizationInterceptor;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufDeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppFactory;
import de.uniluebeck.itm.tr.util.Service;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.Wiseml;

public class WSNServiceHandleFactory {

	public static WSNServiceHandle create(final String secretReservationKey,
										  final TestbedRuntime testbedRuntime,
										  final String urnPrefix,
										  final URL wsnServiceEndpointURL,
										  final String wiseMLFilename,
										  final String[] reservedNodes,
										  final ProtobufDeliveryManager protobufDeliveryManager,
										  final ProtobufControllerServer protobufControllerServer) {

		// De-serialize original WiseML and strip out all nodes that are not part of this reservation
		final Wiseml wiseML = WiseMLHelper.deserialize(WiseMLHelper.readWiseMLFromFile(wiseMLFilename));
		List<Setup.Node> node = wiseML.getSetup().getNode();
		Iterator<Setup.Node> nodeIterator = node.iterator();

		List<String> reservedNodesList = Arrays.asList(reservedNodes);
		while (nodeIterator.hasNext()) {
			Setup.Node currentNode = nodeIterator.next();
			if (!reservedNodesList.contains(currentNode.getId())) {
				nodeIterator.remove();
			}
		}

		final Injector injector = Guice.createInjector(new Module() {
			

			@Override
			public void configure(final Binder binder) {

				binder.bind(TestbedRuntime.class).toProvider(new Provider<TestbedRuntime>() {
					@Override
					public TestbedRuntime get() {
						return testbedRuntime;
					}
				});
				
				// -----------------------------------------
				
				binder.bind(ProtobufDeliveryManager.class).toInstance(protobufDeliveryManager);
				binder.bind(ProtobufControllerServer.class).toInstance(protobufControllerServer);
				binder.bind(String.class).annotatedWith(Names.named("URN_PREFIX")).toInstance(urnPrefix);
				binder.bind(String.class).annotatedWith(Names.named("SECRET_RESERVATION_KEY")).toInstance(secretReservationKey);
				binder.bind(URL.class).annotatedWith(Names.named("WSN_SERVICE_ENDPOINT")).toInstance(wsnServiceEndpointURL);
				binder.bind(Wiseml.class).toInstance(wiseML);
				
				
				// -----------------------------------------
				
				binder.bind(String[].class).annotatedWith(Names.named("RESERVED_NODES")).toInstance(reservedNodes);
				
				binder.bind(WSNApp.class).toInstance(WSNAppFactory.create(testbedRuntime, reservedNodes));

				binder.bind(WSNService.class).to(WSNServiceImpl.class);
				
				binder.bind(WSNService.class).annotatedWith(NonWS.class).to(WSNServiceImplInternal.class);
				
				binder.bind(Service.class).annotatedWith(Names.named("WSN_SERVICE_HANDLE")).to(WSNServiceHandle.class);

				binder.bindInterceptor(Matchers.any(), annotatedWith(de.uniluebeck.itm.tr.iwsn.AuthorizationRequired.class), new IWSNAuthorizationInterceptor());
			}
		});
		

		final WSNService wsnService = injector.getInstance(WSNService.class);
		injector.injectMembers(wsnService);

		return (WSNServiceHandle) injector.getInstance(Key.get(Service.class, Names.named("WSN_SERVICE_HANDLE")));
	}

}