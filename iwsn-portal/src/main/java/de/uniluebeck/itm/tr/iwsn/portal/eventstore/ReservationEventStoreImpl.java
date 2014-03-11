package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import eventstore.IEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

class ReservationEventStoreImpl implements ReservationEventStore {
    private static final Logger log = LoggerFactory.getLogger(ReservationEventStoreImpl.class);

    private Reservation reservation;
    private IEventStore eventStore;

    @Inject
    public ReservationEventStoreImpl(
            @Assisted final Reservation reservation, final PortalEventStoreHelper portalEventStoreHelper) {
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
            reservation.getReservationEventBus().register(this);
        } catch (IOException e) {
            log.error("Can't store event", e);
        }
    }

    @Subscribe
    public void onDevicesAttachedEventFromPortalEventBus(final DevicesAttachedEvent event) {
        storeEvent(event);
    }

    @Subscribe
    public void onUpstreamMessageEventFromPortalEventBus(final UpstreamMessageEvent event) {
        storeEvent(event);
    }

    @Subscribe
    public void onDevicesDetachedEventFromPortalEventBus(final DevicesDetachedEvent event) {
        storeEvent(event);
    }

    @Subscribe
    public void onNotificationEventFromPortalEventBus(final NotificationEvent event) {
        storeEvent(event);
    }

    @Subscribe
    public void onSingleNodeResponseFromPortalEventBus(final SingleNodeResponse response) {
        storeEvent(response);
    }

    @Subscribe
    public void onGetChannelPipelinesResponse(final GetChannelPipelinesResponse response) {
        storeEvent(response);
    }

    @Subscribe
    public void onRequest(final Request request) {
        storeEvent(request);
    }

    private void storeEvent(final MessageLite event) {
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
        this.reservation.getReservationEventBus().unregister(this);
        eventStore.close();
    }
}
