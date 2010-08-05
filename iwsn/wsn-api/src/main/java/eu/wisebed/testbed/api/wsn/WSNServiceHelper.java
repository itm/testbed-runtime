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

import de.uniluebeck.itm.tr.util.FileUtils;
import eu.wisebed.testbed.api.wsn.v211.*;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Small helper class that allows to obtain instances of the several web services.
 */
public class WSNServiceHelper {

	private static final Logger log = Logger.getLogger(WSNServiceHelper.class);

	private static File tmpFileSessionManagement = null;

	private static File tmpFileWSN = null;

	private static File tmpFileController = null;

	private static final Lock tmpFileSessionManagementLock = new ReentrantLock();

	private static final Lock tmpFileWSNLock = new ReentrantLock();

	private static final Lock tmpFileControllerLock = new ReentrantLock();

	/**
	 * Returns the port to the Session Management API.
	 *
	 * @param endpointUrl the endpoint URL to connect to
	 * @return a {@link eu.wisebed.testbed.api.wsn.v211.SessionManagement} instance that is
	 *         connected to the Web Service endpoint
	 */
	public static SessionManagement getSessionManagementService(String endpointUrl) {

		QName qName = new QName("urn:SessionManagementService", "SessionManagementService");
		InputStream resourceStream = WSNServiceHelper.class.getClassLoader().getResourceAsStream("SessionManagementService.wsdl");

		tmpFileSessionManagementLock.lock();
		try {
			if (tmpFileSessionManagement == null) {
				try {
					tmpFileSessionManagement = FileUtils.copyToTmpFile(resourceStream, "tr.iwsn.sessionmanagement", "wsdl");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			tmpFileSessionManagementLock.unlock();
		}

		SessionManagementService service;
		try {
			service = new SessionManagementService(tmpFileSessionManagement.toURI().toURL(), qName);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		SessionManagement sessionManagementPort = service.getSessionManagementPort();

		Map<String, Object> ctxt = ((BindingProvider) sessionManagementPort).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		return sessionManagementPort;

	}

	/**
	 * Returns the port to the Controller API.
	 *
	 * @param endpointUrl the endpoint URL to connect to
	 * @return a {@link eu.wisebed.testbed.api.wsn.v211.Controller} instance that is connected to the Web
	 *         Service endpoint
	 */
	public static Controller getControllerService(String endpointUrl) {

		QName qName = new QName("urn:ControllerService", "ControllerService");
		InputStream resourceStream = WSNServiceHelper.class.getClassLoader().getResourceAsStream("ControllerService.wsdl");

		tmpFileControllerLock.lock();
		try {
			if (tmpFileController == null) {
				try {
					tmpFileController = FileUtils.copyToTmpFile(resourceStream, "tr.iwsn.controller", "wsdl");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			tmpFileControllerLock.unlock();
		}

		ControllerService service;
		try {
			service = new ControllerService(tmpFileController.toURI().toURL(), qName);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		Controller controllerPort = service.getControllerPort();

		Map<String, Object> ctxt = ((BindingProvider) controllerPort).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

		return controllerPort;

	}

	/**
	 * Returns the port to the WSN API instance.
	 *
	 * @param endpointUrl the endpoint URL to connect to
	 * @return a {@link eu.wisebed.testbed.api.wsn.v211.WSN} instance that is connected to the Web Service
	 *         endpoint
	 */
	public static WSN getWSNService(String endpointUrl) {

		QName qName = new QName("urn:WSNService", "WSNService");
		InputStream resourceStream = WSNServiceHelper.class.getClassLoader().getResourceAsStream("WSNService.wsdl");

		tmpFileWSNLock.lock();
		try {
			if (tmpFileWSN == null) {
				try {
					tmpFileWSN = FileUtils.copyToTmpFile(resourceStream, "tr.iwsn.wsn", "wsdl");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			tmpFileWSNLock.unlock();
		}

		WSNService service;
		try {
			service = new WSNService(tmpFileWSN.toURI().toURL(), qName);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

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
