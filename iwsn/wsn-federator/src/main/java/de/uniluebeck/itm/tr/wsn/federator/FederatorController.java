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

package de.uniluebeck.itm.tr.wsn.federator;

import de.uniluebeck.itm.tr.util.TimedCache;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.deliverymanager.DeliveryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@WebService(
		serviceName = "ControllerService",
		targetNamespace = Constants.NAMESPACE_CONTROLLER_SERVICE,
		portName = "ControllerPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_CONTROLLER_SERVICE
)
public class FederatorController implements Controller {

	private static final Logger log = LoggerFactory.getLogger(FederatorController.class);

	private static final int CACHE_TIMEOUT = 10;

	private static final TimeUnit CACHE_TIMEOUT_UNIT = TimeUnit.MINUTES;

	/**
	 * Maps the federatedRequestId to the federatorRequestId (i.e. remote to local)
	 */
	private TimedCache<String, String> requestIdMappingCache = new TimedCache<String, String>(
			CACHE_TIMEOUT,
			CACHE_TIMEOUT_UNIT
	);

	/**
	 * Maps federatedRequestID to a list of received RequestStatus instances (multiple updates for one id possible). This
	 * map caches received RequestStatus instances until the final mapping of federatedRequestID to federatorRequestId is
	 * known. This should normally never happen, but in very fast networks, it may happen that an asynchronous status
	 * update is received before the mapping is set using addRequestIdMapping.
	 */
	private TimedCache<String, LinkedList<RequestStatus>> pendingRequestStatus =
			new TimedCache<String, LinkedList<RequestStatus>>(CACHE_TIMEOUT, CACHE_TIMEOUT_UNIT);

	private String controllerEndpointUrl;

	private Endpoint controllerEndpoint;

	private DeliveryManager deliveryManager;

	public FederatorController(String controllerEndpointUrl) {
		this.controllerEndpointUrl = controllerEndpointUrl;
		this.deliveryManager = new DeliveryManager();
	}

	public void addRequestIdMapping(String federatedRequestId, String federatorRequestId) {

		// Add the mapping to the list
		log.debug("Mapping federatedRequestId {} to federatorRequestId {} = ", federatedRequestId, federatorRequestId);
		requestIdMappingCache.put(federatedRequestId, federatorRequestId);

		// Dispatch potentially received status updates
		LinkedList<RequestStatus> requestStatusList = pendingRequestStatus.get(federatedRequestId);

		if (requestStatusList != null) {
			log.debug("Already got {} status updates for federatedRequestId {} ", requestStatusList.size(),
					federatedRequestId
			);

			// Dispatch all status updates and remove them from the list
			synchronized (requestStatusList) {
				for (RequestStatus status : requestStatusList) {
					changeIdAndDispatch(federatorRequestId, status);
				}
				pendingRequestStatus.remove(federatedRequestId);
			}
		}
	}

	/**
	 * Starts the Controller Web Service endpoint.
	 *
	 * @throws Exception on failure
	 */
	public void start() throws Exception {
		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(controllerEndpointUrl);

		log.debug("Starting federator controller using endpoint URL {} and binding URL {}...",
				controllerEndpointUrl,
				bindAllInterfacesUrl
		);

		controllerEndpoint = Endpoint.publish(bindAllInterfacesUrl, this);
		deliveryManager.start();

		log.debug("Started federator controller on {}!", bindAllInterfacesUrl);
	}

	/**
	 * Stops the Controller Web Service endpoint.
	 *
	 * @throws Exception on failure
	 */
	public void stop() throws Exception {

		log.debug("Calling experimentEnded() on connected controllers...");
		deliveryManager.experimentEnded();

		deliveryManager.stop();

		if (controllerEndpoint != null) {
			log.info("Stopping federator controller at {}...", controllerEndpointUrl);
			controllerEndpoint.stop();
		}
	}

	public void addController(String controllerEndpointUrl) {
		deliveryManager.addController(controllerEndpointUrl);
	}

	public void removeController(String controllerEndpointUrl) {
		deliveryManager.removeController(controllerEndpointUrl);
	}

	private void receive(@WebParam(name = "msg", targetNamespace = "") Message msg) {
		deliveryManager.receive(msg);
	}

	/**
	 * Maps federatedRequestID to a list of received RequestStatus instances (multiple updates for one id possible). This
	 * map caches received RequestStatus instances until the final mapping of federatedRequestID to federatorRequestId is
	 * known. This should normally never happen, but in very fast networks, it may happen that an asynchronous status
	 * update is received before the mapping is set using addRequestIdMapping.
	 *
	 * @param status
	 */
	private void cacheRequestStatus(RequestStatus status) {
		synchronized (pendingRequestStatus) {

			// If no entry for this request id exists, create a new list and add
			// it to the cache
			LinkedList<RequestStatus> requestStatusList = pendingRequestStatus.get(status.getRequestId());

			if (requestStatusList == null) {
				requestStatusList = new LinkedList<RequestStatus>();
				pendingRequestStatus.put(status.getRequestId(), requestStatusList);
			}

			// Append this status to the list for this request ID
			requestStatusList.add(status);
		}
	}

	/**
	 * Change the incoming request ID to the request ID that was issued by the federator to its client.
	 *
	 * @param newRequestId
	 * @param status
	 */
	private void changeIdAndDispatch(String newRequestId, RequestStatus status) {
		status.setRequestId(newRequestId);
		deliveryManager.receiveStatus(status);
	}

	private void receiveStatus(@WebParam(name = "status", targetNamespace = "") RequestStatus status) {

		String federatorRequestId = requestIdMappingCache.get(status.getRequestId());

		if (federatorRequestId != null) {
			// change the incoming request ID to the request ID that was issued
			// by the federator to its client
			changeIdAndDispatch(federatorRequestId, status);

		} else {
			log.warn("Unknown requestId {}. Caching the status update for " + CACHE_TIMEOUT + " " + CACHE_TIMEOUT_UNIT
					+ " until the federatedRequestId <-> federatorRequestIdDropping mapping is known", status
					.getRequestId()
			);
			cacheRequestStatus(status);
		}
	}

	@Override
	public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> messageList) {
		for (Message message : messageList) {
			receive(message);
		}
	}

	@Override
	public void receiveStatus(
			@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> requestStatusList) {
		for (RequestStatus requestStatus : requestStatusList) {
			receiveStatus(requestStatus);
		}
	}

	@Override
	public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") final List<String> notificationList) {
		deliveryManager.receiveNotification(notificationList);
	}

	@Override
	public void experimentEnded() {
		deliveryManager.experimentEnded();
	}

	String getControllerEndpointUrl() {
		return controllerEndpointUrl;
	}
}
