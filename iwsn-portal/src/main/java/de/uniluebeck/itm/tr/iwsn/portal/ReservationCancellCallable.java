package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.messages.ReservationCancelledEvent;

import java.util.concurrent.Callable;

public class ReservationCancellCallable implements Callable<Void> {

    private final PortalEventBus portalEventBus;

    private final Reservation reservation;

    public ReservationCancellCallable(final PortalEventBus portalEventBus, final Reservation reservation) {
        this.portalEventBus = portalEventBus;
        this.reservation = reservation;
    }

    @Override
    public Void call() throws Exception {
        reservation.startAndWait();
        if (reservation.isFinalized()) {
            return null;
        }
        final ReservationCancelledEvent event = ReservationCancelledEvent
                .newBuilder()
                .setSerializedKey(reservation.getSerializedKey())
                .build();
        portalEventBus.post(event);
        return null;
    }
}
