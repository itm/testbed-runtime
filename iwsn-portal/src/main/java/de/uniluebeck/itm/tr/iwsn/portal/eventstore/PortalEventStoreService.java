package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.Service;
import eventstore.IEventContainer;

import java.io.IOException;
import java.util.Iterator;

public interface PortalEventStoreService extends Service {
    public Iterator<IEventContainer> getEvents(final String serializedReservationKey) throws IOException;
    public Iterator<IEventContainer> getEventsBetween(final String serializedReservationKey, long startTime, long endTime) throws IOException;
}
