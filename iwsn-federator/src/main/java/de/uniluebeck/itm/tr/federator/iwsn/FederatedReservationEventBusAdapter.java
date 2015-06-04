package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.common.ReservationHelper;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.*;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters.convertToSOAP;

public class FederatedReservationEventBusAdapter extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(FederatedReservationEventBusAdapter.class);

	private final PortalEventBus portalEventBus;

	private final FederatedReservation reservation;

	@Inject
	public FederatedReservationEventBusAdapter(final PortalEventBus portalEventBus,
											   @Assisted final FederatedReservation reservation) {
		this.portalEventBus = portalEventBus;
		this.reservation = reservation;
	}


	@Override
	protected void doStart() {
		log.trace("FederatedReservationEventBusAdapter.doStart()");
		try {
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("FederatedReservationEventBusAdapter.doStop()");
		try {
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onEvent(final Object obj) {

		log.trace("FederatedReservationEventBusAdapter.on({})", obj);

		if (MessageHeaderPair.isUnwrappedMessageEvent(obj)) {
			MessageHeaderPair pair = MessageHeaderPair.fromUnwrapped(obj);
			Header header = pair.header;
			MessageLite message = pair.message;

			if (!header.hasSerializedReservationKey() ||
					!ReservationHelper.equals(reservation.getSerializedKey(), header.getSerializedReservationKey())) {
				return;
			}

			// this call comes from another internal or external TR component (e.g., the REST API or an external plugin) and
			// results in a call on federated testbeds through calling WSN federator service

			try {

				final long requestId = header.getCorrelationId();
				final List<eu.wisebed.api.v3.wsn.Link> links = newArrayList();
				List<NodeUrn> nodeUrns;
				NodeUrn sourceNodeUrn;
				NodeUrn targetNodeUrn;
				switch (header.getType()) {

					case REQUEST_ARE_NODES_ALIVE:
						nodeUrns = Lists.transform(header.getNodeUrnsList(), NodeUrn::new);
						reservation.getWsnFederatorService().areNodesAlive(requestId, nodeUrns);
						break;

					case REQUEST_ARE_NODES_CONNECTED:
						// handled by FederatorPortalEventBusAdapter
						break;

					case REQUEST_DISABLE_NODES:
						nodeUrns = Lists.transform(header.getNodeUrnsList(), NodeUrn::new);
						reservation.getWsnFederatorService().disableNodes(requestId, nodeUrns);
						break;

					case REQUEST_DISABLE_VIRTUAL_LINKS:
						for (de.uniluebeck.itm.tr.iwsn.messages.Link link : ((DisableVirtualLinksRequest) message).getLinksList()) {
							sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
							targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());
							links.add(new eu.wisebed.api.v3.wsn.Link()
											.withSourceNodeUrn(sourceNodeUrn)
											.withTargetNodeUrn(targetNodeUrn)
							);
						}
						reservation.getWsnFederatorService().disableVirtualLinks(requestId, links);
						break;

					case REQUEST_DISABLE_PHYSICAL_LINKS:
						for (de.uniluebeck.itm.tr.iwsn.messages.Link link : ((DisablePhysicalLinksRequest) message).getLinksList()) {
							sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
							targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());
							links.add(new eu.wisebed.api.v3.wsn.Link()
											.withSourceNodeUrn(sourceNodeUrn)
											.withTargetNodeUrn(targetNodeUrn)
							);
						}
						reservation.getWsnFederatorService().disablePhysicalLinks(requestId, links);
						break;

					case REQUEST_ENABLE_NODES:
						nodeUrns = newArrayList(transform(header.getNodeUrnsList(), NodeUrn::new));
						reservation.getWsnFederatorService().enableNodes(requestId, nodeUrns);
						break;

					case REQUEST_ENABLE_PHYSICAL_LINKS:
						for (de.uniluebeck.itm.tr.iwsn.messages.Link link : ((EnablePhysicalLinksRequest) message).getLinksList()) {
							sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
							targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());
							links.add(new eu.wisebed.api.v3.wsn.Link()
											.withSourceNodeUrn(sourceNodeUrn)
											.withTargetNodeUrn(targetNodeUrn)
							);
						}
						reservation.getWsnFederatorService().enablePhysicalLinks(requestId, links);
						break;

					case REQUEST_ENABLE_VIRTUAL_LINKS:
						final List<VirtualLink> virtualLinks = newArrayList();
						for (de.uniluebeck.itm.tr.iwsn.messages.Link link : ((EnableVirtualLinksRequest) message).getLinksList()) {
							sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
							targetNodeUrn = new NodeUrn(link.getTargetNodeUrn());
							virtualLinks.add(new VirtualLink()
											.withSourceNodeUrn(sourceNodeUrn)
											.withTargetNodeUrn(targetNodeUrn)
							);
							// TODO do something sensible with filters & their parameters and remoteEndpoints
							log.warn("TODO do something sensible with filters & their parameters and remoteEndpoints");
						}
						reservation.getWsnFederatorService().enableVirtualLinks(requestId, virtualLinks);
						break;

					case REQUEST_FLASH_IMAGES:
						final byte[] image = ((FlashImagesRequest) message).getImage().toByteArray();
						nodeUrns = Lists.transform(header.getNodeUrnsList(), NodeUrn::new);
						final FlashProgramsConfiguration configuration = new FlashProgramsConfiguration()
								.withNodeUrns(nodeUrns)
								.withProgram(image);
						final List<FlashProgramsConfiguration> configurations = newArrayList(configuration);
						reservation.getWsnFederatorService().flashPrograms(requestId, configurations);
						break;

					case REQUEST_GET_CHANNEL_PIPELINES:
						nodeUrns = Lists.transform(header.getNodeUrnsList(), NodeUrn::new);
						final List<ChannelPipelinesMap> pipelines =
								reservation.getWsnFederatorService().getChannelPipelines(nodeUrns);
						final
						Map<NodeUrn, de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>
								responseMap = Converters.convertToProto(pipelines);
						for (de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse getChannelPipelineResponse : responseMap
								.values()) {
							reservation.getEventBus().post(getChannelPipelineResponse);
						}
						break;

					case REQUEST_RESET_NODES:
						nodeUrns = Lists.transform(header.getNodeUrnsList(), NodeUrn::new);
						reservation.getWsnFederatorService().resetNodes(requestId, nodeUrns);
						break;

					case REQUEST_SEND_DOWNSTREAM_MESSAGES:
						nodeUrns = Lists.transform(header.getNodeUrnsList(), NodeUrn::new);
						byte[] bytes = ((SendDownstreamMessagesRequest) message).getMessageBytes().toByteArray();
						reservation.getWsnFederatorService().send(requestId, nodeUrns, bytes);
						break;

					case REQUEST_SET_CHANNEL_PIPELINES:
						nodeUrns = Lists.transform(header.getNodeUrnsList(), NodeUrn::new);
						List<ChannelHandlerConfiguration> chcs = convertToSOAP(((SetChannelPipelinesRequest) message)
								.getChannelHandlerConfigurationsList());
						reservation.getWsnFederatorService().setChannelPipeline(requestId, nodeUrns, chcs);
						break;

					default:
						throw new IllegalArgumentException("Unknown request type " + header.getType() + "!");
				}

			} catch (AuthorizationFault | ReservationNotRunningFault_Exception | VirtualizationNotEnabledFault_Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + "@" + Integer.toHexString(hashCode());
	}
}
