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

package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.CommonPreconditions;
import de.uniluebeck.itm.tr.common.PreconditionsFactory;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerController;
import de.uniluebeck.itm.util.SecureIdGenerator;
import de.uniluebeck.itm.util.TimedCache;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.wsn.WSN;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@WebService(
		name = "Controller",
		endpointInterface = "eu.wisebed.api.v3.controller.Controller",
		portName = "ControllerPort",
		serviceName = "ControllerService",
		targetNamespace = "http://wisebed.eu/api/v3/controller"
)
public class FederatorControllerImpl extends AbstractService implements FederatorController {

	private static final Logger log = LoggerFactory.getLogger(FederatorControllerImpl.class);

	private static final int CACHE_TIMEOUT = 10;

	private static final TimeUnit CACHE_TIMEOUT_UNIT = TimeUnit.MINUTES;

	private final URI endpointUri;

	private final CommonPreconditions preconditions;

	private final SchedulerService schedulerService;

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

	private final ServicePublisher servicePublisher;

	private final DeliveryManager deliveryManager;

	private final FederatedEndpoints<WSN> wsnFederatedEndpoints;

	private ServicePublisherService jaxWsService;

	@Inject
	public FederatorControllerImpl(final ServicePublisher servicePublisher,
								   final DeliveryManager deliveryManager,
								   final IWSNFederatorServiceConfig config,
								   final PreconditionsFactory preconditionsFactory,
								   final SchedulerService schedulerService,
								   final SecureIdGenerator secureIdGenerator,
								   @Assisted final FederatedEndpoints<WSN> wsnFederatedEndpoints,
								   @Assisted final Set<NodeUrnPrefix> nodeUrnPrefixes,
								   @Assisted final Set<NodeUrn> nodeUrns) {

		this.servicePublisher = checkNotNull(servicePublisher);
		this.deliveryManager = checkNotNull(deliveryManager);
		this.wsnFederatedEndpoints = checkNotNull(wsnFederatedEndpoints);
		this.schedulerService = checkNotNull(schedulerService);

		this.preconditions = preconditionsFactory.createCommonPreconditions(nodeUrnPrefixes, nodeUrns);

		String uriString;
		uriString = config.getFederatorControllerEndpointUriBase().toString();
		uriString += uriString.endsWith("/") ? "" : "/";
		uriString += secureIdGenerator.getNextId();
		this.endpointUri = URI.create(uriString);
	}

	@Override
	protected void doStart() {

		log.trace("FederatorControllerImpl.doStart()");

		try {

			log.debug("Starting federator controller using endpoint URI {}", endpointUri);

			jaxWsService = servicePublisher.createJaxWsService(endpointUri.getPath(), this);
			jaxWsService.startAndWait();

			log.debug("Starting federator DeliveryManager");
			deliveryManager.startAndWait();

			log.debug("Adding federator controller endpoint to federated reservations");

			for (Map.Entry<WSN, URI> entry : wsnFederatedEndpoints.getEndpointsURIMap().entrySet()) {

				final WSN endpoint = entry.getKey();
				final URI uri = entry.getValue();

				log.trace(
						"Adding federator controller endpoint URI ({}) to federated testbed WSN instance ({})",
						endpointUri.toString(),
						uri
				);

				endpoint.addController(endpointUri.toString());
			}

			notifyStarted();

		} catch (Exception e) {

			log.error("Error while starting federator controller endpoint: ", e);

			if (jaxWsService != null && jaxWsService.isRunning()) {
				jaxWsService.stopAndWait();
			}

			if (deliveryManager.isRunning()) {
				deliveryManager.stopAndWait();
			}

			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			for (Map.Entry<WSN, URI> entry : wsnFederatedEndpoints.getEndpointsURIMap().entrySet()) {
				final WSN endpoint = entry.getKey();
				final URI uri = entry.getValue();
				try {
					endpoint.removeController(endpointUri.toString());
				} catch (Exception e) {
					log.error("Exception while removing federator controller from federated testbed " + uri + ": ", e);
				}
			}

			log.debug("Calling reservationEnded() on connected controllers...");

			deliveryManager.reservationEnded(DateTime.now());
			deliveryManager.stopAndWait();

			if (jaxWsService.isRunning()) {
				log.info("Stopping federator controller at {}...", endpointUri);
				jaxWsService.stopAndWait();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@WebMethod(exclude = true)
	public void addRequestIdMapping(long federatedRequestId, long federatorRequestId) {

		// Add the mapping to the list
		log.debug("Mapping federatedRequestId {} to federatorRequestId {} = ", federatedRequestId, federatorRequestId);
		requestIdMappingCache.put(federatedRequestId, federatorRequestId);

		// Dispatch potentially received status updates
		final LinkedList<RequestStatus> requestStatusList = pendingRequestStatus.get(federatedRequestId);

		if (requestStatusList != null) {
			log.debug("Already got {} status updates for federatedRequestId {} ", requestStatusList.size(),
					federatedRequestId
			);

			// Dispatch all status updates and remove them from the list
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (requestStatusList) {
				for (RequestStatus status : requestStatusList) {
					changeIdAndDispatch(federatorRequestId, status);
				}
				pendingRequestStatus.remove(federatedRequestId);
			}
		}
	}

	@WebMethod(exclude = true)
	public void addController(DeliveryManagerController controller) {
		log.trace("FederatorControllerImpl.addController({})", controller);
		deliveryManager.addController(controller);
	}

	@WebMethod(exclude = true)
	public void removeController(DeliveryManagerController controller) {
		log.trace("FederatorControllerImpl.removeController({})", controller);
		deliveryManager.removeController(controller);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receive", targetNamespace = "http://wisebed.eu/api/v3/controller",
			className = "eu.wisebed.api.v3.controller.Receive")
	public void receive(
			@WebParam(name = "msg", targetNamespace = "")
			List<Message> msg) {
		for (Message message : msg) {
			log.trace("FederatorControllerImpl.receive({})", message);
			receive(message);
		}
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receiveNotification", targetNamespace = "http://wisebed.eu/api/v3/controller",
			className = "eu.wisebed.api.v3.controller.ReceiveNotification")
	public void receiveNotification(
			@WebParam(name = "notifications", targetNamespace = "")
			List<Notification> notifications) {
		log.trace("FederatorControllerImpl.receiveNotification({})", notifications);
		deliveryManager.receiveNotification(notifications);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receiveStatus", targetNamespace = "http://wisebed.eu/api/v3/controller",
			className = "eu.wisebed.api.v3.controller.ReceiveStatus")
	public void receiveStatus(
			@WebParam(name = "status", targetNamespace = "")
			List<RequestStatus> status) {
		log.trace("FederatorControllerImpl.receiveStatus({})", status);
		for (RequestStatus requestStatus : status) {
			receiveStatus(requestStatus);
		}
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "nodesAttached", targetNamespace = "http://wisebed.eu/api/v3/controller",
			className = "eu.wisebed.api.v3.controller.NodesAttached")
	public void nodesAttached(
			@WebParam(name = "timestamp", targetNamespace = "")
			DateTime timestamp,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns) {
		log.trace("FederatorControllerImpl.nodesAttached({}, {})", timestamp, nodeUrns);
		preconditions.checkNodesKnown(nodeUrns);
		deliveryManager.nodesAttached(timestamp, nodeUrns);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "nodesDetached", targetNamespace = "http://wisebed.eu/api/v3/controller",
			className = "eu.wisebed.api.v3.controller.NodesDetached")
	public void nodesDetached(
			@WebParam(name = "timestamp", targetNamespace = "")
			DateTime timestamp,
			@WebParam(name = "nodeUrns", targetNamespace = "")
			List<NodeUrn> nodeUrns) {
		log.trace("FederatorControllerImpl.nodesDetached({}, {})", timestamp, nodeUrns);
		preconditions.checkNodesKnown(nodeUrns);
		deliveryManager.nodesDetached(timestamp, nodeUrns);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "reservationStarted", targetNamespace = "http://wisebed.eu/api/v3/controller",
			className = "eu.wisebed.api.v3.controller.ReservationStarted")
	public void reservationStarted(
			@WebParam(name = "timestamp", targetNamespace = "")
			DateTime timestamp) {
		log.trace("FederatorControllerImpl.reservationStarted({})", timestamp);
		deliveryManager.reservationStarted(timestamp);
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "reservationEnded", targetNamespace = "http://wisebed.eu/api/v3/controller",
			className = "eu.wisebed.api.v3.controller.ReservationEnded")
	public void reservationEnded(
			@WebParam(name = "timestamp", targetNamespace = "")
			DateTime timestamp) {
		log.trace("FederatorControllerImpl.reservationEnded({})", timestamp);
		deliveryManager.reservationEnded(timestamp);
	}

	@WebMethod(exclude = true)
	public URI getEndpointUrl() {
		return endpointUri;
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

	private void receive(final Message msg) {
		preconditions.checkNodesKnown(msg.getSourceNodeUrn());
		deliveryManager.receive(msg);
	}
}
