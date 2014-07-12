package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.eventstore.IEventStore;

import java.io.IOException;

public interface ReservationEventStore extends IEventStore, Service {

	/**
	 * Returns all events belonging to the associated reservation. If the operation has not yet started the iterator
	 * will not contain any elements, if the reservation is still running the iterator is pointing to a sequence that
	 * is (potentially) still growing.
	 *
	 * @return an iterator over the contents of the ReservationEventStore
	 *
	 * @throws java.io.IOException
	 * 		if the underlying persistence layer is broken
	 */
	public CloseableIterator<IEventContainer> getEvents() throws IOException;

	/**
	 * Returns all events of the associated reservation that fall within the given time span between {@code startTime}
	 * and {@code endTime}. If the operation has not yet started or there are no events in the given time span the
	 * iterator will not contain any elements, if the reservation is still running the iterator is pointing to a
	 * sequence that is (potentially) still growing.
	 *
	 * @param startTime
	 * 		begin of the time span
	 * @param endTime
	 * 		end of the time span
	 *
	 * @return an iterator over the contents of the ReservationEventStore between startTime and endTime
	 *
	 * @throws IOException
	 * 		if the underlying persistence layer is broken
	 */
	public CloseableIterator<IEventContainer> getEventsBetween(long startTime, long endTime) throws IOException;
}
