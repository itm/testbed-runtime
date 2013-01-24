package de.uniluebeck.itm.tr.iwsn.portal;

import static com.google.common.base.Preconditions.checkNotNull;

public class ReservationStartedEvent {

	private final Reservation reservation;

	public ReservationStartedEvent(final Reservation reservation) {
		this.reservation = checkNotNull(reservation);
	}

	public Reservation getReservation() {
		return reservation;
	}

	@Override
	public boolean equals(final Object o) {

		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final ReservationStartedEvent that = (ReservationStartedEvent) o;
		return reservation.equals(that.reservation);
	}

	@Override
	public int hashCode() {
		return reservation.hashCode();
	}
}
