/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufControllerServer;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufDeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.util.Service;
import eu.wisebed.api.wsn.WSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class WSNServiceHandle implements Service {

	private static final Logger log = LoggerFactory.getLogger(WSNServiceHandle.class);

	private final String secretReservationKey;

	private final WSNService wsnService;

	private final WSNSoapService wsnSoapService;

	private final WSNApp wsnApp;

	private final URL wsnInstanceEndpointUrl;

	private ProtobufControllerServer protobufControllerServer;

	private final ProtobufDeliveryManager protobufControllerHelper;

	WSNServiceHandle(String secretReservationKey,
					 URL wsnInstanceEndpointUrl,
					 WSNService wsnService,
					 WSNSoapService wsnSoapService,
					 WSNApp wsnApp,
					 ProtobufControllerServer protobufControllerServer,
					 ProtobufDeliveryManager protobufControllerHelper) {

		this.secretReservationKey = secretReservationKey;
		this.wsnService = wsnService;
		this.wsnSoapService = wsnSoapService;
		this.wsnApp = wsnApp;
		this.wsnInstanceEndpointUrl = wsnInstanceEndpointUrl;
		this.protobufControllerServer = protobufControllerServer;
		this.protobufControllerHelper = protobufControllerHelper;
	}

	@Override
	public void start() throws Exception {
		wsnApp.startAndWait();
		wsnService.start();
		wsnSoapService.start();
	}

	@Override
	public void stop() {

		try {
			wsnSoapService.stop();
		} catch (Exception e) {
			log.error("Exception while stopping WSN SOAP Web service interface: {}", e);
		}

		try {
			wsnService.stop();
		} catch (Throwable e) {
			if (e instanceof NullPointerException) {
				// ignore as it is well-known and an error in the jre library
			} else {
				log.warn("" + e, e);
			}
		}

		try {
			wsnApp.stopAndWait();
		} catch (Throwable e) {
			log.warn("" + e, e);
		}

		try {
			protobufControllerServer.stopHandlers(secretReservationKey);
		} catch (Throwable e) {
			log.warn("" + e, e);
		}
	}

	public WSN getWsnService() {
		return wsnService;
	}

	public WSNApp getWsnApp() {
		return wsnApp;
	}

	public URL getWsnInstanceEndpointUrl() {
		return wsnInstanceEndpointUrl;
	}

	public ProtobufDeliveryManager getProtobufControllerHelper() {
		return protobufControllerHelper;
	}

	public String getSecretReservationKey() {
		return secretReservationKey;
	}

}
