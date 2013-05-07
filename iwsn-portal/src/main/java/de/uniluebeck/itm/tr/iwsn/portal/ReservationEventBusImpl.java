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

import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.STRING_TO_NODE_URN;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newDevicesAttachedEvent;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newDevicesDetachedEvent;
import static de.uniluebeck.itm.tr.iwsn.messages.RequestHelper.extractNodeUrns;

public class ReservationEventBusImpl extends AbstractService implements ReservationEventBus {

	private static final Logger log = LoggerFactory.getLogger(ReservationEventBus.class);

	private final PortalEventBus portalEventBus;

	private final EventBus eventBus;

	private final Reservation reservation;

	@Inject
	public ReservationEventBusImpl(final PortalEventBus portalEventBus,
								   final EventBusFactory eventBusFactory,
								   @Assisted final Reservation reservation) {
		this.portalEventBus = portalEventBus;
		this.eventBus = eventBusFactory.create("ReservationEventBus");
		this.reservation = reservation;
	}

	@Override
	public void register(final Object object) {

		log.trace("ReservationEventBus[{}].register(object={})", reservation.getKey(), object);

		eventBus.register(object);
	}

	@Override
	public void unregister(final Object object) {

		log.trace("ReservationEventBus[{}].unregister(object={})", reservation.getKey(), object);

		eventBus.unregister(object);
	}

	@Override
	public void post(final Object event) {

		log.trace("ReservationEventBus[{}].post(event={})", reservation.getKey(), event);

		checkState(isRunning());

		if (event instanceof Request) {
			assertNodesArePartOfReservation(extractNodeUrns((Request) event));
			portalEventBus.post(event);
		}
	}

	@Subscribe
	public void onDevicesAttachedEventFromPortalEventBus(final DevicesAttachedEvent event) {

		log.trace("ReservationEventBus[{}].onDevicesAttachedEventFromPortalEventBus({})", reservation.getKey(), event);

		final Set<NodeUrn> eventNodeUrns = newHashSet(transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
		final Set<NodeUrn> reservedNodeUrnsOfEvent = Sets.filter(eventNodeUrns, in(reservation.getNodeUrns()));

		if (!reservedNodeUrnsOfEvent.isEmpty()) {
			eventBus.post(newDevicesAttachedEvent(event.getTimestamp(), reservedNodeUrnsOfEvent));
		}
	}

	@Subscribe
	public void onUpstreamMessageEventFromPortalEventBus(final UpstreamMessageEvent event) {

		log.trace("ReservationEventBus[{}].onUpstreamMessageEventFromPortalEventBus({})", reservation.getKey(), event);

		final NodeUrn sourceNodeUrn = new NodeUrn(event.getSourceNodeUrn());

		if (reservation.getNodeUrns().contains(sourceNodeUrn)) {
			eventBus.post(event);
		}
	}

	@Subscribe
	public void onDevicesDetachedEventFromPortalEventBus(final DevicesDetachedEvent event) {

		log.trace("ReservationEventBus[{}].onDevicesDetachedEventFromPortalEventBus({})", reservation.getKey(), event);

		final Set<NodeUrn> eventNodeUrns = newHashSet(transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
		final Set<NodeUrn> reservedNodeUrnsOfEvent = Sets.filter(eventNodeUrns, in(reservation.getNodeUrns()));

		if (!reservedNodeUrnsOfEvent.isEmpty()) {
			eventBus.post(newDevicesDetachedEvent(event.getTimestamp(), reservedNodeUrnsOfEvent));
		}
	}

	@Subscribe
	public void onNotificationEventFromPortalEventBus(final NotificationEvent event) {
		log.trace("ReservationEventBus[{}].onNotificationEventFromPortalEventBus({})", reservation.getKey(), event);
		if (!event.hasNodeUrn() || reservation.getNodeUrns().contains(new NodeUrn(event.getNodeUrn()))) {
			eventBus.post(event);
		}
	}

	@Subscribe
	public void onSingleNodeProgressFromPortalEventBus(final SingleNodeProgress progress) {
		log.trace("ReservationEventBus[{}].onSingleNodeProgressFromPortalEventBus({})", reservation.getKey(), progress);
		if (reservation.getKey().equals(progress.getReservationId())) {
			eventBus.post(progress);
		}
	}

	@Subscribe
	public void onSingleNodeResponseFromPortalEventBus(final SingleNodeResponse response) {
		log.trace("ReservationEventBus[{}].onSingleNodeResponseFromPortalEventBus({})", reservation.getKey(), response);
		if (reservation.getKey().equals(response.getReservationId())) {
			eventBus.post(response);
		}
	}

	@Subscribe
	public void onReservationStartedEventFromPortalEventBus(final ReservationStartedEvent event) {
		log.trace("ReservationEventBus[{}].onReservationStartedEventFromPortalEventBus({})", reservation.getKey(), event);
		if (event.getReservation() == reservation) {
			eventBus.post(event);
		}
	}

	@Subscribe
	public void onReservationEndedEventFromPortalEventBus(final ReservationEndedEvent event) {
		log.trace("ReservationEventBus[{}].onReservationEndedEventFromPortalEventBus({})", reservation.getKey(), event);
		if (event.getReservation() == reservation) {
			eventBus.post(event);
		}
	}

	@Override
	protected void doStart() {

		log.trace("ReservationEventBus[{}].doStart()", reservation.getKey());

		try {
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("ReservationEventBus[{}].doStop()", reservation.getKey());

		try {
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public void enableVirtualization() {
		throw new RuntimeException("TODO not yet implemented!");
	}

	@Override
	public void disableVirtualization() {
		throw new RuntimeException("TODO not yet implemented!");
	}

	private void assertNodesArePartOfReservation(final Set<NodeUrn> nodeUrns) {
		if (!reservation.getNodeUrns().containsAll(nodeUrns)) {
			final Set<NodeUrn> unreservedNodeUrns = Sets.filter(nodeUrns, not(in(reservation.getNodeUrns())));
			throw new IllegalArgumentException("The node URNs [" + Joiner.on(",").join(unreservedNodeUrns) + "] "
					+ "are not part of the reservation."
			);
		}
	}
}
