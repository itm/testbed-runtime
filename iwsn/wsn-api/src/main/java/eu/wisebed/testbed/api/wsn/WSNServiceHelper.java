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

package eu.wisebed.testbed.api.wsn;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;

import eu.wisebed.testbed.api.wsn.v211.Controller;
import eu.wisebed.testbed.api.wsn.v211.ControllerService;
import eu.wisebed.testbed.api.wsn.v211.ExperimentNotRunningException;
import eu.wisebed.testbed.api.wsn.v211.ExperimentNotRunningException_Exception;
import eu.wisebed.testbed.api.wsn.v211.SessionManagement;
import eu.wisebed.testbed.api.wsn.v211.SessionManagementService;
import eu.wisebed.testbed.api.wsn.v211.UnknownNodeUrnException;
import eu.wisebed.testbed.api.wsn.v211.UnknownNodeUrnException_Exception;
import eu.wisebed.testbed.api.wsn.v211.UnknownReservationIdException;
import eu.wisebed.testbed.api.wsn.v211.UnknownReservationIdException_Exception;
import eu.wisebed.testbed.api.wsn.v211.WSN;
import eu.wisebed.testbed.api.wsn.v211.WSNService;


/**
 * Small helper class that allows to obtain instances of the several web services.
 */
public class WSNServiceHelper {
	private static final Logger log = Logger.getLogger(WSNServiceHelper.class);

	/**
	 * Returns the port to the Session Management API.
	 *
	 * @param endpointUrl the endpoint URL to connect to
	 *
	 * @return a {@link eu.wisebed.testbed.api.wsn.v211.SessionManagement} instance that is
	 *         connected to the Web Service endpoint
	 */
	public static SessionManagement getSessionManagementService(String endpointUrl) {

		QName qName = new QName("urn:SessionManagementService", "SessionManagementService");
		URL resource = WSNServiceHelper.class.getClassLoader().getResource("SessionManagementService.wsdl");

		log.debug("Creating session management client for " + endpointUrl);
		log.debug("URL to WSDL is " + resource);
		
		SessionManagementService service = new SessionManagementService(resource, qName);
		SessionManagement sessionManagementPort = service.getSessionManagementPort();

		Map<String, Object> ctxt = ((BindingProvider) sessionManagementPort).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		return sessionManagementPort;

	}

	/**
	 * Returns the port to the Controller API.
	 *
	 * @param endpointUrl the endpoint URL to connect to
	 *
	 * @return a {@link eu.wisebed.testbed.api.wsn.v211.Controller} instance that is connected to the Web
	 *         Service endpoint
	 */
	public static Controller getControllerService(String endpointUrl) {

		QName qName = new QName("urn:ControllerService", "ControllerService");
		URL resource = WSNServiceHelper.class.getClassLoader().getResource("ControllerService.wsdl");

		ControllerService service = new ControllerService(resource, qName);
		Controller controllerPort = service.getControllerPort();

		Map<String, Object> ctxt = ((BindingProvider) controllerPort).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		return controllerPort;

	}

	/**
	 * Returns the port to the WSN API instance.
	 *
	 * @param endpointUrl the endpoint URL to connect to
	 *
	 * @return a {@link eu.wisebed.testbed.api.wsn.v211.WSN} instance that is connected to the Web Service
	 *         endpoint
	 */
	public static WSN getWSNService(String endpointUrl) {

		QName qName = new QName("urn:WSNService", "WSNService");
		URL resource = WSNServiceHelper.class.getClassLoader().getResource("WSNService.wsdl");

		WSNService service = new WSNService(resource, qName);
		WSN wsnPort = service.getWSNPort();

		Map<String, Object> ctxt = ((BindingProvider) wsnPort).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		return wsnPort;
	}

	public static ExperimentNotRunningException_Exception createExperimentNotRunningException(String msg,
																										Exception e) {
		ExperimentNotRunningException exception = new ExperimentNotRunningException();
		exception.setMessage(msg);
		return new ExperimentNotRunningException_Exception(msg, exception, e);
	}

	public static UnknownReservationIdException_Exception createUnknownReservationIdException(String msg, String reservationId, Exception e) {

		UnknownReservationIdException exception = new UnknownReservationIdException();
		exception.setMessage(msg);
		exception.setReservationId(reservationId);
		return new UnknownReservationIdException_Exception(msg, exception, e);
	}

	public static UnknownNodeUrnException_Exception createUnknownNodeUrnException(Collection<String> nodeUrns) {
		String msg = "At least one node URNs is unknown. Ignoring request...";
		UnknownNodeUrnException exception = new UnknownNodeUrnException();
		exception.getUrn().addAll(nodeUrns);
		exception.setMessage(msg);
		return new UnknownNodeUrnException_Exception(msg, exception);
	}

}
