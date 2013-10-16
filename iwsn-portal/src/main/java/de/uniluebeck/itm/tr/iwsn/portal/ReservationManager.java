package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.SecretReservationKey;

import java.util.Set;

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
	Reservation getReservation(Set<SecretReservationKey> secretReservationKeys) throws ReservationUnknownException;

	/**
	 * Returns a reservation instance belonging to the given {@code jsonSerializedSecretReservationKeys} or {@code null}
	 * if no reservation with the given key is known. The serialization format follows the one specified in {@link
	 * de.uniluebeck.itm.tr.iwsn.portal.Reservation#getSerializedKey()}.
	 *
	 * @param jsonSerializedSecretReservationKeys
	 * 		the serialized set of secret reservation keys
	 *
	 * @return a reservation instance of {@code null} if not found
	 *
	 * @throws ReservationUnknownException
	 */
	Reservation getReservation(String jsonSerializedSecretReservationKeys) throws ReservationUnknownException;

}
