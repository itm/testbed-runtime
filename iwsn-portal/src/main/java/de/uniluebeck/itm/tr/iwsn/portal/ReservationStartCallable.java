package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;

import java.util.concurrent.Callable;

public class ReservationStartCallable implements Callable<Void> {

	private final PortalEventBus portalEventBus;

	private final Reservation reservation;

	public ReservationStartCallable(final PortalEventBus portalEventBus, final Reservation reservation) {
		this.portalEventBus = portalEventBus;
		this.reservation = reservation;
	}

	@Override
	public Void call() throws Exception {
		reservation.startAndWait();
		final ReservationStartedEvent event = ReservationStartedEvent
				.newBuilder()
				.setSerializedKey(reservation.getSerializedKey())
				.build();
		portalEventBus.post(event);
		return null;
	}
}
