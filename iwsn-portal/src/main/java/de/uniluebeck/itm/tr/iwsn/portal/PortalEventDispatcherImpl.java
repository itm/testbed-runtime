package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStore;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.common.NodeUrnHelper.STRING_TO_NODE_URN;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newDevicesAttachedEvent;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newDevicesDetachedEvent;

public class PortalEventDispatcherImpl extends AbstractService implements PortalEventDispatcher {


    private static final Logger log = LoggerFactory.getLogger(PortalEventDispatcherImpl.class);
    private final PortalEventBus portalEventBus;
    private final ReservationManager reservationManager;
    private final PortalEventStore eventStore;

    @Inject
    public PortalEventDispatcherImpl(final PortalEventBus portalEventBus, final ReservationManager reservationManager, final PortalEventStore eventStore) {
        this.portalEventBus = checkNotNull(portalEventBus);
        this.reservationManager = checkNotNull(reservationManager);
        this.eventStore = checkNotNull(eventStore);
    }

    @Override
    protected void doStart() {
        log.trace("PortalEventDispatcherImpl.doStart()");
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

    @Subscribe
    public void onDevicesAttachedEventFromPortalEventBus(final DevicesAttachedEvent event) {

        log.trace("PortalEventDispatcherImpl.onDevicesAttachedEventFromPortalEventBus({})", event);

        final DateTime time = new DateTime(event.getTimestamp());

        Set<NodeUrn> eventNodeUrns = newHashSet(transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
        Set<NodeUrn> nodeUrnsWithoutReservation = new HashSet<NodeUrn>();
        Optional<Reservation> reservation = findNextReservation(eventNodeUrns, nodeUrnsWithoutReservation, time);

        while (reservation.isPresent()) {

            final Set<NodeUrn> reservedNodeUrnsOfEvent = Sets.filter(eventNodeUrns, in(reservation.get().getNodeUrns()));

            if (!reservedNodeUrnsOfEvent.isEmpty()) {
                reservation.get().getEventBus().post(newDevicesAttachedEvent(event.getTimestamp(), reservedNodeUrnsOfEvent));
            }
            eventNodeUrns.removeAll(Sets.newHashSet(reservedNodeUrnsOfEvent));
            reservation = findNextReservation(eventNodeUrns, nodeUrnsWithoutReservation, time);
        }

        if (!nodeUrnsWithoutReservation.isEmpty()) {
            storeEventToPortalEventStore(event, event.getTimestamp());
        }
    }


    @Subscribe
    public void onUpstreamMessageEventFromPortalEventBus(final UpstreamMessageEvent event) {

        log.trace("PortalEventDispatcherImpl.onUpstreamMessageEventFromPortalEventBus({})", event);

        final NodeUrn sourceNodeUrn = new NodeUrn(event.getSourceNodeUrn());
        final Optional<Reservation> reservation = reservationManager.getReservation(sourceNodeUrn, new DateTime(event.getTimestamp()));

        if (reservation.isPresent()) {
            reservation.get().getEventBus().post(event);
        } else {
            storeEventToPortalEventStore(event, event.getTimestamp());
        }
    }

    @Subscribe
    public void onDevicesDetachedEventFromPortalEventBus(final DevicesDetachedEvent event) {

        log.trace("PortalEventDispatcherImpl.onDevicesDetachedEventFromPortalEventBus({})", event);

        final DateTime time = new DateTime(event.getTimestamp());

        Set<NodeUrn> eventNodeUrns = newHashSet(transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
        Set<NodeUrn> nodeUrnsWithoutReservation = new HashSet<NodeUrn>();
        Optional<Reservation> reservation = findNextReservation(eventNodeUrns, nodeUrnsWithoutReservation, time);

        while (reservation.isPresent()) {
            final Set<NodeUrn> reservedNodeUrnsOfEvent = Sets.filter(eventNodeUrns, in(reservation.get().getNodeUrns()));

            if (!reservedNodeUrnsOfEvent.isEmpty()) {
                reservation.get().getEventBus().post(newDevicesDetachedEvent(event.getTimestamp(), reservedNodeUrnsOfEvent));
            }

            eventNodeUrns.removeAll(Sets.newHashSet(reservedNodeUrnsOfEvent));
            reservation = findNextReservation(eventNodeUrns, nodeUrnsWithoutReservation, time);

        }
        if (!nodeUrnsWithoutReservation.isEmpty()) {
            storeEventToPortalEventStore(event, event.getTimestamp());
        }

    }

    @Subscribe
    public void onNotificationEventFromPortalEventBus(final NotificationEvent event) {

        log.trace("PortalEventDispatcherImpl.onNotificationEventFromPortalEventBus({})", event);

        if (event.hasNodeUrn()) {
            Optional<Reservation> reservation = reservationManager.getReservation(new NodeUrn(event.getNodeUrn()), new DateTime(event.getTimestamp()));
            if (reservation.isPresent()) {
                reservation.get().getEventBus().post(event);
            } else {
                storeEventToPortalEventStore(event, event.getTimestamp());
            }
        } else {
            List<Reservation> reservations = reservationManager.getReservations(new DateTime(event.getTimestamp()));
            for (Reservation reservation : reservations) {
                reservation.getEventBus().post(event);
            }

            if (reservations.isEmpty()) {
                storeEventToPortalEventStore(event, event.getTimestamp());
            }
        }
    }

    @Subscribe
    public void onSingleNodeProgressFromPortalEventBus(final SingleNodeProgress progress) {
        log.trace("PortalEventDispatcherImpl.onSingleNodeProgressFromPortalEventBus({})", progress);

        Reservation reservation = reservationManager.getReservation(progress.getReservationId());
        if (reservation != null) {
            reservation.getEventBus().post(progress);
        } else {
            storeEventToPortalEventStore(progress);
        }
    }

    @Subscribe
    public void onSingleNodeResponseFromPortalEventBus(final SingleNodeResponse response) {
        log.trace("PortalEventDispatcherImpl.onSingleNodeResponseFromPortalEventBus({})", response);

        Reservation reservation = reservationManager.getReservation(response.getReservationId());
        if (reservation != null) {
            reservation.getEventBus().post(response);
        } else {
            storeEventToPortalEventStore(response);
        }
    }

    @Subscribe
    public void onReservationStartedEventFromPortalEventBus(final ReservationStartedEvent event) {
        log.trace("PortalEventDispatcherImpl.onReservationStartedEventFromPortalEventBus({})", event);

        Reservation reservation = reservationManager.getReservation(event.getSerializedKey());
        if (reservation != null) {
            reservation.getEventBus().post(event);
        } else {
            storeEventToPortalEventStore(event);
        }
    }

    @Subscribe
    public void onReservationEndedEventFromPortalEventBus(final ReservationEndedEvent event) {
        log.trace("PortalEventDispatcherImpl.onReservationEndedEventFromPortalEventBus({})", event);
        Reservation reservation = reservationManager.getReservation(event.getSerializedKey());
        if (reservation != null) {
            reservation.getEventBus().post(event);
        } else {
            storeEventToPortalEventStore(event);
        }
    }

    @Subscribe
    public void onReservationCancelledEventFromPortalEventBus(final ReservationCancelledEvent event) {
        log.trace("PortalEventDispatcherImpl.onReservationCancelledEventFromPortalEventBus({})", event);
        Reservation reservation = reservationManager.getReservation(event.getSerializedKey());
        if (reservation != null) {
            reservation.getEventBus().post(event);
        } else {
            storeEventToPortalEventStore(event);
        }
    }

    @Subscribe
    public void onGetChannelPipelinesResponse(final GetChannelPipelinesResponse response) {

        log.trace("PortalEventDispatcherImpl.onGetChannelPipelinesResponse({})", response);

        Reservation reservation = reservationManager.getReservation(response.getReservationId());
        if (reservation != null) {
            reservation.getEventBus().post(response);
        } else {
            storeEventToPortalEventStore(response);
        }
    }

    private void storeEventToPortalEventStore(final MessageLite event, long timestamp) {
        log.trace("PortalEventDispatcherImpl.storeEventToPortalEventStore({})", event);
        try {
            //noinspection unchecked
            eventStore.storeEvent(event, event.getClass(), timestamp);
        } catch (IOException e) {
            log.error("Failed to store event", e);
        }
    }

    private void storeEventToPortalEventStore(final MessageLite event) {
        log.trace("PortalEventDispatcherImpl.storeEventToPortalEventStore({})", event);
        try {
            //noinspection unchecked
            eventStore.storeEvent(event, event.getClass());
        } catch (IOException e) {
            log.error("Failed to store event", e);
        }
    }

    /**
     * Given a set of nodes contained in an event, this methods iterates through the node urns searching a reservation for each node until a reservation is found. All nodes, for which no reservation was found are added to the nodeUrnsWithoutReservation set
     *
     * @param eventNodeUrns              the nodes from the event not yet checked
     * @param nodeUrnsWithoutReservation nodes checked but not part of a reservation
     * @param time                       the time at which the nodes should have been part of a reservation
     * @return the first reservation found if any
     */
    private Optional<Reservation> findNextReservation(Set<NodeUrn> eventNodeUrns, Set<NodeUrn> nodeUrnsWithoutReservation, DateTime time) {
        eventNodeUrns.removeAll(nodeUrnsWithoutReservation);

        for (NodeUrn urn : eventNodeUrns) {
            Optional<Reservation> reservation = reservationManager.getReservation(urn, time);
            if (reservation.isPresent()) {
                return reservation;
            } else {
                nodeUrnsWithoutReservation.add(urn);
            }
        }
        return Optional.absent();

    }

}
