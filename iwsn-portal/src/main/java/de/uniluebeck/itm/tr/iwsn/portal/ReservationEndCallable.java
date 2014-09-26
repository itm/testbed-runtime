package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.messages.ReservationClosedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;

import java.util.concurrent.Callable;

public class ReservationEndCallable implements Callable<Void> {

    private final PortalEventBus portalEventBus;

    private final Reservation reservation;

    public ReservationEndCallable(final PortalEventBus portalEventBus, final Reservation reservation) {
        this.portalEventBus = portalEventBus;
        this.reservation = reservation;
    }

    @Override
    public Void call() throws Exception {
        if (reservation.isCancelled() || reservation.isFinalized()) {
            return null;
        }

            final ReservationEndedEvent event = ReservationEndedEvent
                    .newBuilder()
                    .setSerializedKey(reservation.getSerializedKey())
                    .setTimestamp(reservation.getInterval().getEndMillis())
                    .build();
            portalEventBus.post(event);

        return null;
    }
}
