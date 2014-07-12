package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;

import java.io.IOException;

public interface PortalEventStoreService extends Service {

	/**
	 * Returns the {@link de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStore} belonging to the given
	 * reservation or creates a new one if none is currently existing.
	 *
	 * @param reservation
	 * 		the reservation for which to retrieve the store
	 *
	 * @return an existing or newly created ReservationEventStore
	 */
	public ReservationEventStore getOrCreateReservationEventStore(final Reservation reservation) throws IOException;

	/**
	 * Returns all events belonging to the reservation with key {@code serializedReservationKey}. If the operation has
	 * not yet started the iterator will not contain any elements, if the reservation is still running the iterator is
	 * pointing to a sequence that is (potentially) still growing.
	 *
	 * @param serializedReservationKey
	 * 		the serialized key identifying the reservation
	 *
	 * @return an iterator over the contents of the ReservationEventStore
	 *
	 * @throws IOException
	 * 		if the underlying persistence layer is broken
	 */
	public CloseableIterator<IEventContainer> getEvents(final String serializedReservationKey) throws IOException;

	/**
	 * Returns all events of the given reservation with key {@code serializedReservationKey} that fall within the given
	 * time span between {@code startTime} and {@code endTime}. If the operation has not yet started or there are no
	 * events in the given time span the iterator will not contain any elements, if the reservation is still running
	 * the iterator is pointing to a sequence that is (potentially) still growing.
	 *
	 * @param serializedReservationKey
	 * 		the serialized key identifying the reservation
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
	public CloseableIterator<IEventContainer> getEventsBetween(final String serializedReservationKey, long startTime,
															   long endTime) throws IOException;
}
