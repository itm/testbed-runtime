package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.EventContainer;
import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;

import java.io.IOException;

public interface PortalEventStore extends Service {

	CloseableIterator<MessageHeaderPair> getEventsBetweenTimestamps(long from, long to) throws IOException;

	CloseableIterator<MessageHeaderPair> getEventsFromTimestamp(long from) throws IOException;

	CloseableIterator<MessageHeaderPair> getAllEvents() throws IOException;

}
