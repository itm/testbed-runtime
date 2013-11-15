package de.uniluebeck.itm.tr.federator.iwsn;

public class FederatedReservationScopedEvent {

	private final Object event;

	private final FederatedReservation reservation;

	public FederatedReservationScopedEvent(final Object event,
										   final FederatedReservation reservation) {
		this.event = event;
		this.reservation = reservation;
	}

	public Object getEvent() {
		return event;
	}

	public FederatedReservation getReservation() {
		return reservation;
	}
}
