package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import eventstore.IEventContainer;
import eventstore.IEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

class PortalEventStoreServiceImpl extends AbstractService implements PortalEventStoreService {

    private static final Logger log = LoggerFactory.getLogger(PortalEventStoreServiceImpl.class);
    private final HashMap<String, ReservationEventStore> reservationStores = new HashMap<String, ReservationEventStore>();
    private final PortalEventBus portalEventBus;
    private final ReservationEventStoreFactory reservationEventStoreFactory;
    private final PortalEventStoreHelper portalEventStoreHelper;
    private final Object reservationStoresLock = new Object();

    @Inject
    public PortalEventStoreServiceImpl(final PortalEventBus portalEventBus,
                                       final ReservationEventStoreFactory reservationEventStoreFactory, final PortalEventStoreHelper portalEventStoreHelper) {
        this.portalEventBus = portalEventBus;
        this.reservationEventStoreFactory = reservationEventStoreFactory;
        this.portalEventStoreHelper = portalEventStoreHelper;
    }

    @Override
    protected void doStart() {
        log.trace("PortalEventStoreServiceImpl.doStart()");
        try {
            portalEventBus.register(this);
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        log.trace("PortalEventStoreServiceImpl.doStop()");
        try {
            for (ReservationEventStore store : reservationStores.values()) {
                store.stop();
            }
            portalEventBus.unregister(this);
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Subscribe
    public void onReservationStarted(final ReservationStartedEvent event) {
        ReservationEventStore reservationEventStore = reservationEventStoreFactory.create(event.getReservation());
        log.trace("PortalEventStoreServiceImpl.onReservationStarted(): ReservationEventStore: {}", reservationEventStore); // TODO remove when working
        synchronized (reservationStoresLock) {
            reservationStores.put(event.getReservation().getSerializedKey(), reservationEventStore);
        }
        log.info("Reservation Key is {}", event.getReservation().getSerializedKey()); // TODO remove later
        reservationEventStore.reservationStarted(event);
    }

    @Subscribe
    public void onReservationEnded(final ReservationEndedEvent event) {
        log.trace("PortalEventStoreServiceImpl.onReservationEnded()"); // TODO remove when working
        ReservationEventStore reservationEventStore;
        synchronized (reservationStoresLock) {
            reservationEventStore = reservationStores.remove(event.getReservation().getSerializedKey());
        }
        if (reservationEventStore != null) {
            reservationEventStore.reservationEnded(event);
        }
    }

    private IEventStore getEventStore(String serializedReservationKey) throws IOException {
        ReservationEventStore reservationEventStore;
        synchronized (reservationStoresLock) {
            reservationEventStore = reservationStores.get(serializedReservationKey);
        }
        IEventStore eventStore = null;
        if (reservationEventStore != null) { // ongoing reservation
            eventStore = reservationEventStore.getEventStore();
        } else {
            String totalName = portalEventStoreHelper.eventstoreBasenameForReservation(serializedReservationKey) + ".data";
            log.trace("Reservation NOT ongoing. total path to chronicle = {}", totalName);
            if (new File(totalName).exists()) {
                eventStore = portalEventStoreHelper.createAndConfigureEventStore(serializedReservationKey);
            } else {
                log.warn("Can't open chronicle with base {}", portalEventStoreHelper.eventstoreBasenameForReservation(serializedReservationKey));
            }
        }

        if (eventStore == null) {
            throw new IOException("Can't open event store for key " + serializedReservationKey);
        }

        return eventStore;
    }

    @Override
    public Iterator<IEventContainer> getEventsBetween(String serializedReservationKey, long startTime, long endTime) throws IOException {
        IEventStore store = getEventStore(serializedReservationKey);
        // TODO think about event store closing
        return store.getEventsBetweenTimestamps(startTime, endTime);
    }

    @Override
    public Iterator<IEventContainer> getEvents(String serializedReservationKey) throws IOException {
        IEventStore store = getEventStore(serializedReservationKey);
        // TODO think about event store closing
        return store.getAllEvents();
    }
}
