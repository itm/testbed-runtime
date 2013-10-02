package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerInternalController;
import de.uniluebeck.itm.tr.iwsn.messages.Link;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Controller;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.controller.SingleNodeRequestStatus;
import eu.wisebed.api.v3.wsn.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.STRING_TO_NODE_URN;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters.convertToSOAP;

public class FederatedReservationEventBusAdapter extends AbstractService implements Controller {

	private static final Logger log = LoggerFactory.getLogger(FederatedReservationEventBusAdapter.class);

	private final DeliveryManagerInternalController dmController = new DeliveryManagerInternalController(this);

	private final SessionManagementFederatorService smFederatorService;

	private final Reservation reservation;

	private final ReservationEventBus reservationEventBus;

	private final FederatorController federatorController;

	private final WSNFederatorService wsnFederatorService;

	@Inject
	public FederatedReservationEventBusAdapter(
			final SessionManagementFederatorService smFederatorService,
			@Assisted final Reservation reservation,
			@Assisted final ReservationEventBus reservationEventBus,
			@Assisted final FederatorController federatorController,
			@Assisted final WSNFederatorService wsnFederatorService) {
		this.smFederatorService = smFederatorService;
		this.reservation = reservation;
		this.reservationEventBus = reservationEventBus;
		this.federatorController = federatorController;
		this.wsnFederatorService = wsnFederatorService;
	}


	@Override
	protected void doStart() {
		try {
			federatorController.addController(dmController);
			reservationEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			reservationEventBus.unregister(this);
			federatorController.removeController(dmController);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onRequest(final Request request) {

		// this call comes from another internal or external TR component (e.g., the REST API or an external plugin) and
		// results in a call on federated testbeds through calling WSN federator service

		try {

			final long requestId = request.getRequestId();
			final List<eu.wisebed.api.v3.wsn.Link> links = newArrayList();
			List<NodeUrn> nodeUrns;
			NodeUrn sourceNodeUrn;
			NodeUrn targetNodeUrn;
			switch (request.getType()) {

				case ARE_NODES_ALIVE:
					nodeUrns = newArrayList(
							transform(request.getAreNodesAliveRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
					);
					wsnFederatorService.areNodesAlive(requestId, nodeUrns);
					break;

				case ARE_NODES_CONNECTED:
					// ignore, this is handled by FederatorPortalEventBusAdapter
					break;

				case DISABLE_NODES:
					nodeUrns = newArrayList(
							transform(request.getDisableNodesRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
					);
					wsnFederatorService.disableNodes(requestId, nodeUrns);
					break;

				case DISABLE_VIRTUAL_LINKS:
					for (Link link : request.getDisableVirtualLinksRequest().getLinksList()) {
						sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
						targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());
						links.add(new eu.wisebed.api.v3.wsn.Link()
								.withSourceNodeUrn(sourceNodeUrn)
								.withTargetNodeUrn(targetNodeUrn)
						);
					}
					wsnFederatorService.disableVirtualLinks(requestId, links);
					break;

				case DISABLE_PHYSICAL_LINKS:
					for (Link link : request.getDisablePhysicalLinksRequest().getLinksList()) {
						sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
						targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());
						links.add(new eu.wisebed.api.v3.wsn.Link()
								.withSourceNodeUrn(sourceNodeUrn)
								.withTargetNodeUrn(targetNodeUrn)
						);
					}
					wsnFederatorService.disablePhysicalLinks(requestId, links);
					break;

				case ENABLE_NODES:
					nodeUrns = newArrayList(
							transform(request.getEnableNodesRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
					);
					wsnFederatorService.enableNodes(requestId, nodeUrns);
					break;

				case ENABLE_PHYSICAL_LINKS:
					for (Link link : request.getEnablePhysicalLinksRequest().getLinksList()) {
						sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
						targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());
						links.add(new eu.wisebed.api.v3.wsn.Link()
								.withSourceNodeUrn(sourceNodeUrn)
								.withTargetNodeUrn(targetNodeUrn)
						);
					}
					wsnFederatorService.enablePhysicalLinks(requestId, links);
					break;

				case ENABLE_VIRTUAL_LINKS:
					final List<VirtualLink> virtualLinks = newArrayList();
					for (Link link : request.getEnableVirtualLinksRequest().getLinksList()) {
						sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
						targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());
						virtualLinks.add(new VirtualLink()
								.withSourceNodeUrn(sourceNodeUrn)
								.withTargetNodeUrn(targetNodeUrn)
						);
						// TODO do something sensible with filters & their parameters and remoteEndpoints
						log.warn("TODO do something sensible with filters & their parameters and remoteEndpoints");
					}
					wsnFederatorService.enableVirtualLinks(requestId, virtualLinks);
					break;

				case FLASH_IMAGES:
					final byte[] image = request.getFlashImagesRequest().getImage().toByteArray();
					nodeUrns = newArrayList(
							transform(request.getFlashImagesRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
					);
					final FlashProgramsConfiguration configuration = new FlashProgramsConfiguration()
							.withNodeUrns(nodeUrns)
							.withProgram(image);
					final List<FlashProgramsConfiguration> configurations = newArrayList(configuration);
					wsnFederatorService.flashPrograms(requestId, configurations);
					break;

				case GET_CHANNEL_PIPELINES:
					nodeUrns = newArrayList(
							transform(request.getGetChannelPipelinesRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
					);
					final List<ChannelPipelinesMap> pipelines = wsnFederatorService.getChannelPipelines(nodeUrns);
					final
					Map<NodeUrn, de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>
							responseMap = Converters.convertToProto(pipelines);
					for (de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse getChannelPipelineResponse : responseMap
							.values()) {
						reservationEventBus.post(getChannelPipelineResponse);
					}
					break;

				case RESET_NODES:
					nodeUrns = newArrayList(
							transform(request.getResetNodesRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
					);
					wsnFederatorService.resetNodes(requestId, nodeUrns);
					break;

				case SEND_DOWNSTREAM_MESSAGES:
					nodeUrns = newArrayList(transform(
							request.getSendDownstreamMessagesRequest().getTargetNodeUrnsList(),
							STRING_TO_NODE_URN
					)
					);
					wsnFederatorService.send(requestId, nodeUrns,
							request.getSendDownstreamMessagesRequest().getMessageBytes().toByteArray()
					);
					break;

				case SET_CHANNEL_PIPELINES:
					nodeUrns = newArrayList(
							transform(request.getSetChannelPipelinesRequest().getNodeUrnsList(), STRING_TO_NODE_URN)
					);
					wsnFederatorService.setChannelPipeline(
							requestId,
							nodeUrns,
							convertToSOAP(request.getSetChannelPipelinesRequest().getChannelHandlerConfigurationsList())
					);
					break;

				default:
					throw new IllegalArgumentException("Unknown request type " + request.getType() + "!");
			}

		} catch (AuthorizationFault authorizationFault) {
			throw new IllegalStateException(authorizationFault);
		} catch (ReservationNotRunningFault_Exception e) {
			throw new IllegalStateException(e);
		} catch (VirtualizationNotEnabledFault_Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void nodesAttached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final List<NodeUrn> nodeUrns) {

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		reservationEventBus.post(newDevicesAttachedEvent(timestamp.getMillis(), nodeUrns));
	}

	@Override
	public void nodesDetached(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final List<NodeUrn> nodeUrns) {

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		reservationEventBus.post(newDevicesDetachedEvent(timestamp.getMillis(), nodeUrns));
	}

	@Override
	public void reservationStarted(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		reservationEventBus.post(new ReservationStartedEvent(reservation));
	}

	@Override
	public void reservationEnded(@WebParam(name = "timestamp", targetNamespace = "") final DateTime timestamp) {

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		reservationEventBus.post(new ReservationEndedEvent(reservation));
	}

	@Override
	public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> msgs) {

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		for (Message msg : msgs) {
			reservationEventBus.post(newUpstreamMessageEvent(
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

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		for (Notification notification : notifications) {
			reservationEventBus.post(newNotificationEvent(
					notification.getNodeUrn(),
					notification.getTimestamp().getMillis(),
					notification.getMsg()
			)
			);
		}
	}

	@Override
	public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> statuses) {

		// this call comes from a federated testbed (through the WSN federator controller) and results in posting an
		// event to the federators internal event bus which is then consumed by e.g., the REST API

		for (RequestStatus requestStatus : statuses) {
			for (SingleNodeRequestStatus singleNodeRequestStatus : requestStatus.getSingleNodeRequestStatus()) {

				final String reservationId = reservation.getSerializedKey();
				final long requestId = requestStatus.getRequestId();
				final NodeUrn nodeUrn = singleNodeRequestStatus.getNodeUrn();
				final Integer value = singleNodeRequestStatus.getValue();
				final String msg = singleNodeRequestStatus.getMsg();

				reservationEventBus.post(singleNodeRequestStatus.isCompleted() ?
						newSingleNodeResponse(reservationId, requestId, nodeUrn, value, msg) :
						newSingleNodeProgress(reservationId, requestId, nodeUrn, value)
				);
			}
		}
	}
}
