package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.SecretReservationKey;

import java.util.List;

public interface ReservationManager extends Service {

	/**
	 * Returns a Reservation instance belonging to the given {@code secretReservationKeys} or {@code null} if no
	 * reservation with the given key is known.
	 *
	 * @param secretReservationKeys
	 * 		the reservation key
	 *
	 * @return a {@link Reservation} instance
	 *
	 * @throws ReservationUnknownException
	 * 		if the reservation key is unknown
	 */
	Reservation getReservation(List<SecretReservationKey> secretReservationKeys) throws ReservationUnknownException;

}
