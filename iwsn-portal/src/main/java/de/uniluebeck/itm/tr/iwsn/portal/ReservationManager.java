package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;

public interface ReservationManager extends Service {

	/**
	 * Returns a Reservation instance belonging to the given {@code secretReservationKey} or {@code null} if no
	 * reservation with the given key is known.
	 *
	 * @param secretReservationKey
	 * 		the reservation key
	 *
	 * @return a {@link Reservation} instance
	 *
	 * @throws ReservationUnknownException
	 * 		if the reservation key is unknown
	 */
	Reservation getReservation(long secretReservationKey) throws ReservationUnknownException;

}
