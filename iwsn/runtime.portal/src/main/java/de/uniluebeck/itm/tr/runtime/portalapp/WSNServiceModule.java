/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.internal.Nullable;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerHelper;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppImpl;
import eu.wisebed.ns.wiseml._1.Setup;
import eu.wisebed.ns.wiseml._1.Wiseml;
import eu.wisebed.testbed.api.wsn.ControllerHelper;

import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class WSNServiceModule extends AbstractModule {

	static final String SECRET_RESERVATION_KEY = "wsnservicemodule.secretreservationkey";

	static final String URN_PREFIX = "wsnservicemodule.urnprefix";

	static final String WSN_SERVICE_ENDPOINT_URL = "wsnservicemodule.wsnserviceendpointurl";

	static final String CONTROLLER_SERVICE_ENDPOINT_URL = "wsnservicemodule.controllerserviceendpointuRL";

	static final String WISEML = "wsnservicemodule.wiseml";

	static final String RESERVED_NODES = "wsnservicemodule.reservednodes";

	public static class Factory {

		public static WSNServiceHandle create(String secretReservationKey,
											  TestbedRuntime testbedRuntime,
											  String urnPrefix,
											  URL wsnServiceEndpointURL,
											  URL controllerServiceEndpointURL,
											  String wiseMLFilename,
											  @Nullable String[] reservedNodes,
											  ProtobufControllerHelper protobufControllerHelper,
											  @Nullable ProtobufControllerServer protobufControllerServer) {

			Injector injector = Guice.createInjector(new WSNServiceModule(
					secretReservationKey,
					testbedRuntime,
					urnPrefix,
					wsnServiceEndpointURL,
					controllerServiceEndpointURL,
					wiseMLFilename,
					reservedNodes,
					protobufControllerHelper,
					protobufControllerServer
			)
			);

			return injector.getInstance(WSNServiceHandle.class);
		}

	}

	private String secretReservationKey;

	private TestbedRuntime testbedRuntime;

	private String urnPrefix;

	private URL wsnServiceEndpointURL;

	private URL controllerServiceEndpointURL;

	private Wiseml wiseML;

	private String[] reservedNodes;

	private ProtobufControllerHelper protobufControllerHelper;

	private ProtobufControllerServer protobufControllerServer;

	private WSNServiceModule(String secretReservationKey,
							 TestbedRuntime testbedRuntime,
							 String urnPrefix,
							 URL wsnServiceEndpointURL,
							 URL controllerServiceEndpointURL,
							 String wiseMLFilename,
							 @Nullable String[] reservedNodes,
							 ProtobufControllerHelper protobufControllerHelper,
							 @Nullable ProtobufControllerServer protobufControllerServer) {

		this.secretReservationKey = secretReservationKey;
		this.testbedRuntime = testbedRuntime;
		this.urnPrefix = urnPrefix;
		this.wsnServiceEndpointURL = wsnServiceEndpointURL;
		this.controllerServiceEndpointURL = controllerServiceEndpointURL;
		this.reservedNodes = reservedNodes;
		this.protobufControllerHelper = protobufControllerHelper;
		this.protobufControllerServer = protobufControllerServer;

		// De-serialize original WiseML and strip out all nodes that are not part of this reservation
		this.wiseML = WiseMLHelper.deserialize(WiseMLHelper.readWiseMLFromFile(wiseMLFilename));
		List<Setup.Node> node = this.wiseML.getSetup().getNode();
		Iterator<Setup.Node> nodeIterator = node.iterator();

		List<String> reservedNodesList = Arrays.asList(reservedNodes);
		while (nodeIterator.hasNext()) {
			Setup.Node currentNode = nodeIterator.next();
			if (!reservedNodesList.contains(currentNode.getId())) {
				nodeIterator.remove();
			}
		}

	}

	@Override
	protected void configure() {

		bind(WSNService.class).to(WSNServiceImpl.class);
		bind(WSNApp.class).to(WSNAppImpl.class);

		bind(TestbedRuntime.class).toInstance(testbedRuntime);

		bind(String.class).annotatedWith(Names.named(SECRET_RESERVATION_KEY)).toInstance(secretReservationKey);
		bind(String.class).annotatedWith(Names.named(URN_PREFIX)).toInstance(urnPrefix);
		bind(URL.class).annotatedWith(Names.named(WSN_SERVICE_ENDPOINT_URL)).toInstance(wsnServiceEndpointURL);
		bind(URL.class).annotatedWith(Names.named(CONTROLLER_SERVICE_ENDPOINT_URL))
				.toInstance(controllerServiceEndpointURL);
		bind(Wiseml.class).annotatedWith(Names.named(WISEML)).toInstance(wiseML);
		bind(String[].class).annotatedWith(Names.named(RESERVED_NODES)).toInstance(reservedNodes);
		bind(ProtobufControllerServer.class).toInstance(protobufControllerServer);
		bind(ControllerHelper.class).toInstance(protobufControllerHelper);

	}

}
