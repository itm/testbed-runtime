package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.EventContainer;
import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

class PortalEventStoreImpl extends AbstractService implements PortalEventStore {

    private static final Logger log = LoggerFactory.getLogger(PortalEventStoreImpl.class);
    private final PortalEventStoreHelper portalEventStoreHelper;
    private final PortalServerConfig portalServerConfig;
    private EventStore eventStore;


    @Inject
    public PortalEventStoreImpl(final PortalEventStoreHelper portalEventStoreHelper, final PortalServerConfig portalServerConfig) {
        this.portalEventStoreHelper = portalEventStoreHelper;
        this.portalServerConfig = portalServerConfig;
    }

    @Override
    protected void doStart() {
        log.trace("PortalEventStoreServiceImpl.doStart()");
        try {
            if (portalEventStoreHelper.eventStoreExistsForReservation(portalServerConfig.getPortalEventstoreName())) {
                eventStore = portalEventStoreHelper.loadEventStore(portalServerConfig.getPortalEventstoreName(), false);
            } else {
                eventStore = portalEventStoreHelper.createAndConfigureEventStore(portalServerConfig.getPortalEventstoreName());
            }
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        log.trace("PortalEventStoreServiceImpl.doStop()");
        try {
            close();
        } catch (IOException e) {
            log.warn("Exception on closing event store.", e);
        }
        try {
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    public void storeEvent(@Nonnull Object o) throws IOException, UnsupportedOperationException, IllegalArgumentException {
        if (!isRunning()) {
            return;
        }
        //noinspection unchecked
        eventStore.storeEvent(o);
    }

    @Override
    public void storeEvent(@Nonnull Object object, long timestamp) throws IOException, UnsupportedOperationException, IllegalArgumentException {
        if (!isRunning()) {
            return;
        }
        //noinspection unchecked
        eventStore.storeEvent(object, timestamp);
    }

    @Override
    public void storeEvent(@Nonnull Object o, Class aClass) throws IOException, UnsupportedOperationException, IllegalArgumentException {
        if (!isRunning()) {
            return;
        }
        //noinspection unchecked
        eventStore.storeEvent(o, aClass);
    }

    @Override
    public void storeEvent(@Nonnull Object object, Class type, long timestamp) throws IOException, UnsupportedOperationException, IllegalArgumentException {
        if (!isRunning()) {
            return;
        }
        //noinspection unchecked
        eventStore.storeEvent(object, type, timestamp);
    }


    @Override
    public CloseableIterator<EventContainer> getEventsBetweenTimestamps(long from, long to) throws IOException {
        //noinspection unchecked
        return eventStore.getEventsBetweenTimestamps(from, to);
    }

    @Override
    public CloseableIterator<EventContainer> getEventsFromTimestamp(long from) throws IOException {
        //noinspection unchecked
        return eventStore.getEventsFromTimestamp(from);
    }

    @Override
    public CloseableIterator<EventContainer> getAllEvents() throws IOException {
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
        eventStore.close();
    }
}



