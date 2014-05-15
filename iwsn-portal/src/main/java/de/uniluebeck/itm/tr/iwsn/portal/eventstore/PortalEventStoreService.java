package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;

import java.io.IOException;
import java.util.Iterator;

public interface PortalEventStoreService extends Service {
    public CloseableIterator<IEventContainer> getEvents(final String serializedReservationKey) throws IOException;
    public CloseableIterator<IEventContainer> getEventsBetween(final String serializedReservationKey, long startTime, long endTime) throws IOException;
}
