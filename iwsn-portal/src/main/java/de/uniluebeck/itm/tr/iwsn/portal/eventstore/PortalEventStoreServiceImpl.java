package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationStartedEvent;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.eventstore.IEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

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
            portalEventBus.unregister(this);
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Subscribe
    public void onReservationStarted(final ReservationStartedEvent event) {
        synchronized (reservationStoresLock) {
            ReservationEventStore reservationEventStore = reservationEventStoreFactory.create(event.getReservation());
            reservationEventStore.startAndWait();
            reservationStores.put(event.getReservation().getSerializedKey(), reservationEventStore);
            reservationEventStore.reservationStarted(event);
        }
    }

    @Subscribe
    public void onReservationEnded(final ReservationEndedEvent event) {
        synchronized (reservationStoresLock) {
            ReservationEventStore reservationEventStore;
            reservationEventStore = reservationStores.remove(event.getReservation().getSerializedKey());
            if (reservationEventStore != null) {
                reservationEventStore.reservationEnded(event);
                reservationEventStore.stop();
            }
        }
    }

    private IEventStore getEventStore(String serializedReservationKey) throws IOException {
        synchronized (reservationStoresLock) {
            ReservationEventStore reservationEventStore;
            reservationEventStore = reservationStores.get(serializedReservationKey);
            IEventStore eventStore = null;
            if (reservationEventStore != null) { // ongoing reservation
                eventStore = reservationEventStore;
            } else {
                log.trace("Reservation {} NOT ongoing. Loading persisted event store", serializedReservationKey);
                if (portalEventStoreHelper.eventStoreExistsForReservation(serializedReservationKey)) {
                    eventStore = portalEventStoreHelper.loadEventStore(serializedReservationKey);
                } else {
                    log.warn("Event Store isn't existing for reservation {}", serializedReservationKey);
                }
            }

            if (eventStore == null) {
                throw new IOException("Can't open event store for key " + serializedReservationKey);
            }

            return eventStore;
        }
    }

    @Override
    public CloseableIterator<IEventContainer> getEventsBetween(String serializedReservationKey, long startTime, long endTime) throws IOException {
        IEventStore store = getEventStore(serializedReservationKey);
		//noinspection unchecked
		return store.getEventsBetweenTimestamps(startTime, endTime);
    }

    @Override
    public CloseableIterator<IEventContainer> getEvents(String serializedReservationKey) throws IOException {
        IEventStore store = getEventStore(serializedReservationKey);
		//noinspection unchecked
		return store.getAllEvents();
    }
}
