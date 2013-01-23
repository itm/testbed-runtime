package de.uniluebeck.itm.tr.iwsn.portal;

import java.util.concurrent.Callable;

public class ReservationStopCallable implements Callable<Void> {

	private final Reservation reservation;

	public ReservationStopCallable(final Reservation reservation) {
		this.reservation = reservation;
	}

	@Override
	public Void call() throws Exception {
		reservation.stopAndWait();
		return null;
	}
}
