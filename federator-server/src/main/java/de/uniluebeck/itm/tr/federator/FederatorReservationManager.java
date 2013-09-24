package de.uniluebeck.itm.tr.federator;

import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;

public class FederatorReservationManager extends AbstractService implements ReservationManager {

	@Override
	protected void doStart() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	protected void doStop() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public Reservation getReservation(final String secretReservationKey) throws ReservationUnknownException {
		throw new RuntimeException("Implement me!");
	}
}
