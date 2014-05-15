package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.eventstore.IEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

class ReservationEventStoreImpl implements ReservationEventStore {
    private static final Logger log = LoggerFactory.getLogger(ReservationEventStoreImpl.class);

    private IEventStore eventStore;
    private Reservation reservation;
    private final ReservationEventBus reservationEventBus;

    @Inject
    public ReservationEventStoreImpl(
            @Assisted final Reservation reservation, final PortalEventStoreHelper portalEventStoreHelper) {
        reservationEventBus = reservation.getReservationEventBus();
        this.reservation = reservation;
        try {
            eventStore = portalEventStoreHelper.createAndConfigureEventStore(reservation.getSerializedKey());
        } catch (FileNotFoundException e) {
            log.error("Can't create event store at this location!", e);
        }
    }


    @Override
    public void reservationStarted(final ReservationStartedEvent event) {
        try {
            eventStore.storeEvent(event);
            reservationEventBus.register(this);
        } catch (IOException e) {
            log.error("Can't store event", e);
        }
    }

    @Subscribe
    public void onEvent(final DevicesAttachedEvent event) {
        storeEvent(event);
    }

    @Subscribe
    public void onEvent(final UpstreamMessageEvent event) {
        storeEvent(event);
    }

    @Subscribe
    public void onEvent(final DevicesDetachedEvent event) {
        storeEvent(event);
    }

    @Subscribe
    public void onEvent(final NotificationEvent event) {
        storeEvent(event);
    }

    @Subscribe
    public void onEvent(final SingleNodeResponse response) {
        storeEvent(response);
    }

    @Subscribe
    public void onEvent(final GetChannelPipelinesResponse response) {
        storeEvent(response);
    }

    @Subscribe
    public void onRequest(final Request request) {
        storeEvent(request);
    }

    private void storeEvent(final MessageLite event) {
        log.trace("ReservationEventStoreImpl.storeEvent({})", event);
        try {
            eventStore.storeEvent(event, event.getClass());
        } catch (IOException e) {
            log.error("Failed to store event", e);
        }
    }

    @Override
    public IEventStore getEventStore() {
        return eventStore;
    }

    @Override
    public void reservationEnded(ReservationEndedEvent event) {
        try {
            eventStore.storeEvent(event);
        } catch (IOException e) {
            log.error("Error on reservationEnded()", e);
        }
        stop();
    }

    @Override
    public void stop() {
        log.trace("ReservationEventStoreImpl.stop()");

        reservationEventBus.unregister(this);
        try {
            eventStore.close();
        } catch (IOException e) {
            log.warn("Exception on closing event store.", e);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + reservation.getSerializedKey() + "]";
    }
}
