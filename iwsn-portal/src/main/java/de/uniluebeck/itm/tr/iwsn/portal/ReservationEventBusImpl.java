package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.iwsn.common.NodeUrnHelper.STRING_TO_NODE_URN;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newDevicesAttachedEvent;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newDevicesDetachedEvent;

public class ReservationEventBusImpl extends AbstractService implements ReservationEventBus {

	private static final Logger log = LoggerFactory.getLogger(ReservationEventBus.class);

	private final PortalEventBus portalEventBus;

	private final EventBus eventBus;

	private final Set<NodeUrn> reservedNodeUrns;

	@Inject
	public ReservationEventBusImpl(final PortalEventBus portalEventBus,
								   final EventBus eventBus,
								   @Assisted final Set<NodeUrn> reservedNodeUrns) {
		this.portalEventBus = portalEventBus;
		this.eventBus = eventBus;
		this.reservedNodeUrns = reservedNodeUrns;
	}

	@Override
	public void register(final Object object) {
		log.trace("ReservationEventBusImpl.register(object={})", object);
		checkState(isRunning());
		eventBus.register(object);
	}

	@Override
	public void unregister(final Object object) {
		log.trace("ReservationEventBusImpl.unregister(object={}", object);
		checkState(isRunning());
		eventBus.unregister(object);
	}

	@Override
	public void post(final Object event) {

		log.trace("ReservationEventBusImpl.post(event={})", event);
		checkState(isRunning());

		if (event instanceof Request) {

			final Request request = (Request) event;
			switch (request.getType()) {

				case ARE_NODES_ALIVE:
					throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(
							request.getAreNodesAliveRequest().getNodeUrnsList()
					);
					break;

				case ARE_NODES_CONNECTED:
					throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(
							request.getAreNodesConnectedRequest().getNodeUrnsList()
					);
					break;

				case DISABLE_NODES:
					throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(
							request.getDisableNodesRequest().getNodeUrnsList()
					);
					break;

				case DISABLE_VIRTUAL_LINKS:
					throwIllegalArgumentExceptionIfAnyLinkSourceNodeIsNotPartOfReservation(
							request.getDisableVirtualLinksRequest().getLinksList()
					);
					break;

				case DISABLE_PHYSICAL_LINKS:
					throwIllegalArgumentExceptionIfAnyLinkSourceNodeIsNotPartOfReservation(
							request.getDisablePhysicalLinksRequest().getLinksList()
					);
					break;

				case ENABLE_NODES:
					throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(
							request.getEnableNodesRequest().getNodeUrnsList()
					);
					break;

				case ENABLE_PHYSICAL_LINKS:
					throwIllegalArgumentExceptionIfAnyLinkSourceNodeIsNotPartOfReservation(
							request.getEnablePhysicalLinksRequest().getLinksList()
					);
					break;

				case ENABLE_VIRTUAL_LINKS:
					throwIllegalArgumentExceptionIfAnyLinkSourceNodeIsNotPartOfReservation(
							request.getEnableVirtualLinksRequest().getLinksList()
					);
					break;

				case FLASH_IMAGES:
					throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(
							request.getFlashImagesRequest().getNodeUrnsList()
					);
					break;

				case RESET_NODES:
					throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(
							request.getResetNodesRequest().getNodeUrnsList()
					);
					break;

				case SEND_DOWNSTREAM_MESSAGES:
					throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(
							request.getSendDownstreamMessagesRequest().getTargetNodeUrnsList()
					);
					break;

				case SET_CHANNEL_PIPELINES:
					throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(
							request.getSetChannelPipelinesRequest().getNodeUrnsList()
					);
					break;
			}
		}

		portalEventBus.post(event);
	}

	private void throwIllegalArgumentExceptionIfAnyLinkSourceNodeIsNotPartOfReservation(final List<Link> links) {
		final Set<NodeUrn> requestNodeUrns = newHashSet();
		for (Link link : links) {
			final NodeUrn sourceNodeUrn = new NodeUrn(link.getSourceNodeUrn());
			requestNodeUrns.add(sourceNodeUrn);
		}
		throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(requestNodeUrns);
	}

	private void throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(final Set<NodeUrn> nodeUrns) {
		if (!reservedNodeUrns.containsAll(nodeUrns)) {
			final Set<NodeUrn> unreservedNodeUrns = Sets.filter(nodeUrns, not(in(reservedNodeUrns)));
			throw new IllegalArgumentException("The node URNs [" + Joiner.on(",").join(unreservedNodeUrns) + "] "
					+ "are not part of the reservation.");
		}
	}

	private void throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(final List<String> nodeUrnStrings) {
		final Set<NodeUrn> requestNodeUrns = newHashSet(transform(nodeUrnStrings, STRING_TO_NODE_URN));
		throwIllegalArgumentExceptionIfAnyNodeIsNotPartOfReservation(requestNodeUrns);
	}

	@Subscribe
	public void onDevicesAttachedEventFromPortalEventBus(final DevicesAttachedEvent event) {

		final Set<NodeUrn> eventNodeUrns = newHashSet(transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
		final Set<NodeUrn> reservedNodeUrnsOfEvent = Sets.filter(eventNodeUrns, in(reservedNodeUrns));

		if (!reservedNodeUrnsOfEvent.isEmpty()) {
			eventBus.post(newDevicesAttachedEvent(event.getTimestamp(), reservedNodeUrnsOfEvent));
		}
	}

	@Subscribe
	public void onUpstreamMessageEventFromPortalEventBus(final UpstreamMessageEvent event) {

		final NodeUrn sourceNodeUrn = new NodeUrn(event.getSourceNodeUrn());

		if (reservedNodeUrns.contains(sourceNodeUrn)) {
			eventBus.post(event);
		}
	}

	@Subscribe
	public void onDevicesDetachedEventFromPortalEventBus(final DevicesDetachedEvent event) {

		final Set<NodeUrn> eventNodeUrns = newHashSet(transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
		final Set<NodeUrn> reservedNodeUrnsOfEvent = Sets.filter(eventNodeUrns, in(reservedNodeUrns));

		if (!reservedNodeUrnsOfEvent.isEmpty()) {
			eventBus.post(newDevicesDetachedEvent(event.getTimestamp(), reservedNodeUrnsOfEvent));
		}
	}

	@Subscribe
	public void onNotificationEventFromPortalEventBus(final NotificationEvent event) {
		if (!event.hasNodeUrn() || reservedNodeUrns.contains(new NodeUrn(event.getNodeUrn()))) {
			eventBus.post(event);
		}
	}

	@Subscribe
	public void onSingleNodeProgressFromPortalEventBus(final SingleNodeProgress progress) {
		if (reservedNodeUrns.contains(new NodeUrn(progress.getNodeUrn()))) {
			eventBus.post(progress);
		}
	}

	@Subscribe
	public void onSingleNodeResponseFromPortalEventBus(final SingleNodeResponse response) {
		if (reservedNodeUrns.contains(new NodeUrn(response.getNodeUrn()))) {
			eventBus.post(response);
		}
	}

	@Override
	protected void doStart() {
		try {
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
