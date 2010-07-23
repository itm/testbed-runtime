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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.v211.Message;
import eu.wisebed.testbed.api.wsn.v211.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.JAXB;
import javax.xml.ws.Endpoint;
import java.io.StringWriter;

@Singleton
@WebService(
		serviceName = "ControllerService",
		targetNamespace = Constants.NAMESPACE_CONTROLLER_SERVICE,
		portName = "ControllerPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_CONTROLLER_SERVICE
)
class ControllerServiceImpl implements ControllerService {

	private static final String API_URL = "controller";

	private static final Logger log = LoggerFactory.getLogger(ControllerService.class);

	private String endpointUrl;

	private Endpoint endpoint;

	@Inject
	public ControllerServiceImpl(@Named(PortalModule.NAME_WSN_INSTANCE_BASE_URL) String wisebedBaseUrl) {
		this.endpointUrl = wisebedBaseUrl + API_URL;
	}

	@Override
	public void start() throws Exception {

		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(endpointUrl);

		log.debug("Starting WISEBED controller service...");
		log.debug("Endpoint URL: {}", endpointUrl);
		log.debug("Binding  URL: {}", bindAllInterfacesUrl);

		endpoint = Endpoint.publish(bindAllInterfacesUrl, this);

		log.info("Started WISEBED controller service on {}", bindAllInterfacesUrl);
	}

	@Override
	public void stop() {

		if (endpoint != null) {
			endpoint.stop();
			log.info("Stopped WISEBED controller service on {}", endpointUrl);
		}

	}

	@Override
	public void receive(@WebParam(name = "msg", targetNamespace = "") Message msg) {
		StringWriter stringWriter = new StringWriter();
		JAXB.marshal(msg, stringWriter);
		log.info("Received controller message: {}", stringWriter.toString());
	}

	@Override
	public void receiveStatus(@WebParam(name = "status", targetNamespace = "") RequestStatus status) {
		StringWriter stringWriter = new StringWriter();
		JAXB.marshal(status, stringWriter);
		log.info("Received controller status message: {}", stringWriter.toString());
	}

}
