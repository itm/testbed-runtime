package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.DefaultEventContainer;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

class ReservationEventStoreImpl extends AbstractService implements ReservationEventStore {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventStoreImpl.class);

    private final PortalEventStoreHelper helper;

    private IEventStore eventStore;

    private Reservation reservation;

    @Inject
    public ReservationEventStoreImpl(final PortalEventStoreHelper helper, @Assisted final Reservation reservation) {
        this.helper = helper;
        this.reservation = reservation;
    }

    @Override
    protected void doStart() {
        log.trace("ReservationEventStoreImpl.doStart()");
        try {
            // if service is restarted while a reservation is running (or after the store has been created but the
            // reservation didn't start yet we might have to load an existing instead of creating a new store...
            if (helper.eventStoreExistsForReservation(reservation.getSerializedKey())) {
                eventStore = helper.loadEventStore(reservation.getSerializedKey(), false);
            } else {
                eventStore = helper.createAndConfigureEventStore(reservation.getSerializedKey());
            }
            reservation.getEventBus().register(this);
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        log.trace("ReservationEventStoreImpl.doStop()");
        reservation.getEventBus().unregister(this);
        try {
            eventStore.close();
        } catch (IOException e) {
            log.warn("Exception on closing event store.", e);
        }
        notifyStopped();
    }


    @Subscribe
    public void onEvent(final DevicesAttachedEvent event) {
        storeEvent(event, event.getTimestamp());
    }

    @Subscribe
    public void onEvent(final DevicesDetachedEvent event) {
        storeEvent(event, event.getTimestamp());
    }

    @Subscribe
    public void onEvent(final UpstreamMessageEvent event) {
        storeEvent(event, event.getTimestamp());
    }

    @Subscribe
    public void onEvent(final NotificationEvent event) {
        storeEvent(event, event.getTimestamp());
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
            storeEvent(event, event.getClass());
        } catch (IOException e) {
            log.error("Failed to store event", e);
        }
    }

    private void storeEvent(final MessageLite event, long timestamp) {
        log.trace("ReservationEventStoreImpl.storeEvent({})", event);
        try {
            storeEvent(event, event.getClass(), timestamp);
        } catch (IOException e) {
            log.error("Failed to store event", e);
        }
    }

    @Override
    public void storeEvent(@Nonnull Object object) throws IOException {
        //noinspection unchecked
        eventStore.storeEvent(object);
    }

    @Override
    public void storeEvent(@Nonnull Object object, long timestamp) throws IOException, UnsupportedOperationException, IllegalArgumentException {
        //noinspection unchecked
        eventStore.storeEvent(object, timestamp);
    }

    @Override
    public void storeEvent(@Nonnull Object object, Class type) throws IOException {
        //noinspection unchecked
        eventStore.storeEvent(object, type);
    }

    @Override
    public void storeEvent(@Nonnull Object object, Class type, long timestamp) throws IOException, UnsupportedOperationException, IllegalArgumentException {
        //noinspection unchecked
        eventStore.storeEvent(object, type, timestamp);
    }

    @Override
    public CloseableIterator<IEventContainer> getEventsBetweenTimestamps(long fromTime, long toTime)
            throws IOException {
        //noinspection unchecked
        return eventStore.getEventsBetweenTimestamps(fromTime, toTime);
    }

    @Override
    public CloseableIterator<IEventContainer> getEventsFromTimestamp(long fromTime) throws IOException {
        //noinspection unchecked
        return eventStore.getEventsFromTimestamp(fromTime);
    }

    @Override
    public CloseableIterator<IEventContainer> getAllEvents() throws IOException {
        //noinspection unchecked
        return eventStore.getAllEvents();
    }

    @Override
    public long actualPayloadByteSize() throws IOException {
        return eventStore.actualPayloadByteSize();
    }

    @Override
    public long size() {
        return eventStore.size();
    }

    @Override
    public boolean isEmpty() {
        return eventStore.isEmpty();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    @Override
    public String toString() {
        return ReservationEventStoreImpl.class.getName() + "[" + reservation.getSerializedKey() + "]";
    }

    @Override
    public CloseableIterator<IEventContainer> getEvents() throws IOException {
        //noinspection unchecked
        return new EventIterator(eventStore.getAllEvents());
    }

    @Override
    public CloseableIterator<IEventContainer> getEventsBetween(long startTime, long endTime) throws IOException {
        //noinspection unchecked
        return new EventIterator(eventStore.getEventsBetweenTimestamps(startTime, endTime), startTime, endTime);
    }


    private class EventIterator implements CloseableIterator<IEventContainer> {

        private final CloseableIterator<IEventContainer> underlyingIterator;
        private boolean shouldCreateStartedEvent;
        private boolean shouldCreateEndedEvent;

        EventIterator(CloseableIterator<IEventContainer> underlyingIterator) {
            this(underlyingIterator, reservation.getInterval().getStartMillis(), reservation.getInterval().getEndMillis());
        }

        EventIterator(CloseableIterator<IEventContainer> underlyingIterator, final long startTime, final long endTime) {
            this.underlyingIterator = underlyingIterator;
            shouldCreateStartedEvent = (startTime <= reservation.getInterval().getStartMillis());
            shouldCreateEndedEvent = (endTime >= reservation.getInterval().getEndMillis());
        }

        @Override
        public void close() throws IOException {
            underlyingIterator.close();
        }

        @Override
        public boolean hasNext() {
            return underlyingIterator.hasNext() || shouldCreateStartedEvent || shouldCreateEndedEventNow();
        }

        @Override
        public IEventContainer next() {
            IEventContainer container = null;
            if (shouldCreateStartedEvent) {
                container = new DefaultEventContainer(ReservationStartedEvent.newBuilder().setSerializedKey(reservation.getSerializedKey()).build(), reservation.getInterval().getStartMillis());
                shouldCreateStartedEvent = false;
            } else if (underlyingIterator.hasNext()) {
                return underlyingIterator.next();
            } else if (shouldCreateEndedEventNow()) {
                container = new DefaultEventContainer(ReservationEndedEvent.newBuilder().setSerializedKey(reservation.getSerializedKey()).build(), reservation.getInterval().getEndMillis());
                shouldCreateEndedEvent = false;
            }
            return container;
        }

        private boolean shouldCreateEndedEventNow() {
            return shouldCreateEndedEvent && !reservation.getInterval().getEnd().isAfterNow();
        }

        @Override
        public void remove() {
            underlyingIterator.remove();
        }
    }
}
