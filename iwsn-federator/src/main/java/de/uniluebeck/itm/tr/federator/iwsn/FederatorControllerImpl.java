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
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import de.uniluebeck.itm.util.SecureIdGenerator;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.controller.SingleNodeRequestStatus;
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
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;

@WebService(
		name = "Controller",
		endpointInterface = "eu.wisebed.api.v3.controller.Controller",
		portName = "ControllerPort",
		serviceName = "ControllerService",
		targetNamespace = "http://wisebed.eu/api/v3/controller"
)
public class FederatorControllerImpl extends AbstractService implements FederatorController {

	private static final Logger log = LoggerFactory.getLogger(FederatorControllerImpl.class);

	private final URI endpointUri;

	private final FederatedReservation reservation;

	private final ServicePublisher servicePublisher;

	private final DeliveryManager deliveryManager;

	private final FederatedEndpoints<WSN> wsnFederatedEndpoints;

	private final PortalEventBus portalEventBus;

	private ServicePublisherService jaxWsService;

	@Inject
	public FederatorControllerImpl(final ServicePublisher servicePublisher,
								   final IWSNFederatorServiceConfig config,
								   final SecureIdGenerator secureIdGenerator,
								   final PortalEventBus portalEventBus,
								   @Assisted final DeliveryManager deliveryManager,
								   @Assisted final FederatedReservation reservation,
								   @Assisted final FederatedEndpoints<WSN> wsnFederatedEndpoints) {

		this.reservation = checkNotNull(reservation);
		this.servicePublisher = checkNotNull(servicePublisher);
		this.deliveryManager = checkNotNull(deliveryManager);
		this.wsnFederatedEndpoints = checkNotNull(wsnFederatedEndpoints);
		this.portalEventBus = checkNotNull(portalEventBus);

		String uriString;
		uriString = config.getFederatorControllerEndpointUriBase().toString();
		uriString += uriString.endsWith("/") ? "" : "/";
		uriString += secureIdGenerator.getNextId();
		this.endpointUri = URI.create(uriString);
	}

	@Override
	protected void doStart() {

		log.trace("FederatorControllerImpl[{}].doStart()", reservation.getSerializedKey());

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

	@Override
	public void nodesAttached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final List<NodeUrn> nodeUrns) {

		log.trace("FederatedReservationEventBusAdapter.nodesAttached({}, {})", timestamp, nodeUrns);

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		portalEventBus.post(newDevicesAttachedEvent(timestamp.getMillis(), nodeUrns));
	}

	@Override
	public void nodesDetached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final List<NodeUrn> nodeUrns) {

		log.trace("FederatedReservationEventBusAdapter.nodesDetached({}, {})\", timestamp, nodeUrns");

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		portalEventBus.post(newDevicesDetachedEvent(timestamp.getMillis(), nodeUrns));
	}

	@Override
	public void reservationStarted(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {

		log.trace("FederatedReservationEventBusAdapter.reservationStarted({})", timestamp);

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		portalEventBus.post(new ReservationStartedEvent(reservation));
	}

	@Override
	public void reservationEnded(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {

		log.trace("FederatedReservationEventBusAdapter.reservationEnded({})", timestamp);

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		portalEventBus.post(new ReservationEndedEvent(reservation));
	}

	@Override
	@WebMethod
	@Oneway
	@RequestWrapper(localName = "receive", targetNamespace = "http://wisebed.eu/api/v3/controller",
			className = "eu.wisebed.api.v3.controller.Receive")
	public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> messages) {

		log.trace("FederatedReservationEventBusAdapter.receive({})", messages);

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		for (Message msg : messages) {
			portalEventBus.post(newUpstreamMessageEvent(
					msg.getSourceNodeUrn(),
					msg.getBinaryData(),
					msg.getTimestamp()
			)
			);
		}
	}

	@Override
	public void receiveNotification(
			@WebParam(name = "notifications", targetNamespace = "") final List<Notification> notifications) {

		log.trace("FederatedReservationEventBusAdapter.receiveNotification({})", notifications);

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		for (Notification notification : notifications) {
			portalEventBus.post(newNotificationEvent(
					notification.getNodeUrn(),
					notification.getTimestamp().getMillis(),
					notification.getMsg()
			)
			);
		}
	}

	@Override
	public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> statuses) {

		log.trace("FederatedReservationEventBusAdapter.receiveStatus({})", statuses);

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		for (RequestStatus requestStatus : statuses) {
			for (SingleNodeRequestStatus singleNodeRequestStatus : requestStatus.getSingleNodeRequestStatus()) {

				final String reservationId = reservation.getSerializedKey();
				final long requestId = requestStatus.getRequestId();
				final NodeUrn nodeUrn = singleNodeRequestStatus.getNodeUrn();
				final Integer value = singleNodeRequestStatus.getValue();
				final String msg = singleNodeRequestStatus.getMsg();

				portalEventBus.post(singleNodeRequestStatus.isCompleted() ?
						newSingleNodeResponse(reservationId, requestId, nodeUrn, value, msg) :
						newSingleNodeProgress(reservationId, requestId, nodeUrn, value)
				);
			}
		}
	}

	public URI getEndpointUrl() {
		return endpointUri;
	}

	@Override
	public String toString() {
		return getClass().getName() + "@" + Integer.toHexString(hashCode());
	}
}
