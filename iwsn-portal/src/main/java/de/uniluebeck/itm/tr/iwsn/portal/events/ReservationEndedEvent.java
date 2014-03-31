package de.uniluebeck.itm.tr.iwsn.portal.events;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;

import static com.google.common.base.Preconditions.checkNotNull;

public class ReservationEndedEvent {

	private final Reservation reservation;

	public ReservationEndedEvent(final Reservation reservation) {
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

		final ReservationEndedEvent that = (ReservationEndedEvent) o;
		return reservation.equals(that.reservation);
	}

	@Override
	public int hashCode() {
		return reservation.hashCode();
	}
}
