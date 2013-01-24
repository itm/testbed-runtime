package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.rs.ReservationNotFoundFault_Exception;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault_Exception;

import javax.annotation.Nullable;

public interface ReservationManager extends Service {

	/**
	 * Returns a Reservation instance belonging to the given {@code secretReservationKey} or {@code null} if no
	 * reservation with the given key is known.
	 *
	 * @param secretReservationKey
	 * 		the reservation key
	 *
	 * @return a {@link Reservation} instance or {@code null} if the key is unknown
	 */
	@Nullable
	Reservation getReservation(String secretReservationKey)
			throws RSFault_Exception, ReservationNotFoundFault_Exception, ExperimentNotRunningFault_Exception;

}
