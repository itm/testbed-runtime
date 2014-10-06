package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;
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
	 * Returns a reservation instance belonging to the given {@code jsonSerializedSecretReservationKeys} or {@code
	 * null}
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

	/**
	 * Returns a reservation instance of a reservation that is or was active during {@code timestamp} and that
	 * contains / contained {@code nodeUrn} as reserved node.
	 *
	 * @param nodeUrn
	 * 		a node URN that must be part of the reservation
	 * @param timestamp
	 * 		an instant in time for at which the reservation is active
	 *
	 * @return a reservation instance or none if no reservation matching the criteria is found
	 */
	Optional<Reservation> getReservation(NodeUrn nodeUrn, DateTime timestamp);


    /**
     * Returns a list of reservation instance that are or were active during {@code timestamp}
     * @param timestamp an instant in time for at which the reservations are active
     *
     * @return a list of reservation instances matching the criteria
     */
    List<Reservation> getReservations(DateTime timestamp);


    Collection<Reservation> getNonFinalizedReservations();

}
