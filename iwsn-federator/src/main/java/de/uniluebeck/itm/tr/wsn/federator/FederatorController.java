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

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.util.TimedCache;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Controller;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@WebService(
		name = "Controller",
		endpointInterface = "eu.wisebed.api.v3.controller.Controller",
		portName = "ControllerPort",
		serviceName = "ControllerService",
		targetNamespace = "http://wisebed.eu/api/v3/controller"
)
public class FederatorController extends AbstractService implements Service, Controller {

	private static final Logger log = LoggerFactory.getLogger(FederatorController.class);

	private static final int CACHE_TIMEOUT = 10;

	private static final TimeUnit CACHE_TIMEOUT_UNIT = TimeUnit.MINUTES;

	/**
	 * Maps the federatedRequestId to the federatorRequestId (i.e. remote to local)
	 */
	private TimedCache<Long, Long> requestIdMappingCache = new TimedCache<Long, Long>(
			CACHE_TIMEOUT,
			CACHE_TIMEOUT_UNIT
	);

	/**
	 * Maps federatedRequestID to a list of received RequestStatus instances (multiple updates for one id possible). This
	 * map caches received RequestStatus instances until the final mapping of federatedRequestID to federatorRequestId is
	 * known. This should normally never happen, but in very fast networks, it may happen that an asynchronous status
	 * update is received before the mapping is set using addRequestIdMapping.
	 */
	private final TimedCache<Long, LinkedList<RequestStatus>> pendingRequestStatus =
			new TimedCache<Long, LinkedList<RequestStatus>>(CACHE_TIMEOUT, CACHE_TIMEOUT_UNIT);

	private final URI controllerEndpointUrl;

	private final DeliveryManager deliveryManager;

	private Endpoint controllerEndpoint;

	public FederatorController(URI controllerEndpointUrl) {
		this.controllerEndpointUrl = controllerEndpointUrl;
		this.deliveryManager = new DeliveryManager();
	}

	public void addRequestIdMapping(long federatedRequestId, long federatorRequestId) {

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

	@Override
	protected void doStart() {

		try {

			log.debug("Starting federator controller using endpoint URL {}...", controllerEndpointUrl);

			controllerEndpoint = Endpoint.publish(controllerEndpointUrl.toString(), this);
			deliveryManager.startAndWait();

			log.debug("Started federator controller on {}!", controllerEndpointUrl);

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			log.debug("Calling reservationEnded() on connected controllers...");
			deliveryManager.reservationEnded();

			deliveryManager.stopAndWait();

			if (controllerEndpoint != null) {
				log.info("Stopping federator controller at {}...", controllerEndpointUrl);
				controllerEndpoint.stop();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	public void addController(String controllerEndpointUrl) {
		deliveryManager.addController(controllerEndpointUrl);
	}

	public void removeController(String controllerEndpointUrl) {
		deliveryManager.removeController(controllerEndpointUrl);
	}

	private void receive(final Message msg) {
		deliveryManager.receive(msg);
	}

	/**
	 * Maps federatedRequestID to a list of received RequestStatus instances (multiple updates for one id possible). This
	 * map caches received RequestStatus instances until the final mapping of federatedRequestID to federatorRequestId is
	 * known. This should normally never happen, but in very fast networks, it may happen that an asynchronous status
	 * update is received before the mapping is set using addRequestIdMapping.
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
	 */
	private void changeIdAndDispatch(long newRequestId, RequestStatus status) {
		status.setRequestId(newRequestId);
		deliveryManager.receiveStatus(status);
	}

	private void receiveStatus(final RequestStatus status) {

		Long federatorRequestId = requestIdMappingCache.get(status.getRequestId());

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
	public void receive(final List<Message> messageList) {
		for (Message message : messageList) {
			receive(message);
		}
	}

	@Override
	public void receiveNotification(final List<Notification> notifications) {
		deliveryManager.receiveNotification(notifications);
	}

	@Override
	public void receiveStatus(final List<RequestStatus> requestStatusList) {
		for (RequestStatus requestStatus : requestStatusList) {
			receiveStatus(requestStatus);
		}
	}

	@Override
	public void nodesAttached(final List<NodeUrn> nodeUrns) {
		deliveryManager.nodesAttached(nodeUrns);
	}

	@Override
	public void nodesDetached(final List<NodeUrn> nodeUrns) {
		deliveryManager.nodesDetached(nodeUrns);
	}

	@Override
	public void reservationStarted() {
		// TODO implement
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public void reservationEnded() {
		deliveryManager.reservationEnded();
	}

	URI getControllerEndpointUrl() {
		return controllerEndpointUrl;
	}
}
