package de.uniluebeck.itm.tr.iwsn.portal;

import java.util.concurrent.Callable;

public class ReservationStartCallable implements Callable<Void> {

	private final Reservation reservation;

	public ReservationStartCallable(final Reservation reservation) {
		this.reservation = reservation;
	}

	@Override
	public Void call() throws Exception {
		reservation.startAndWait();
		return null;
	}
}
