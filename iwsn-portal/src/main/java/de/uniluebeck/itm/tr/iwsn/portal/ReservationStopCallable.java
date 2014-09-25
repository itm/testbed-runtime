package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.messages.ReservationClosedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;

import java.util.concurrent.Callable;

public class ReservationStopCallable implements Callable<Void> {

    private final PortalEventBus portalEventBus;

    private final Reservation reservation;
    private final boolean createStopEvent;

    public ReservationStopCallable(final PortalEventBus portalEventBus, final Reservation reservation, final boolean createStopEvent) {
        this.portalEventBus = portalEventBus;
        this.reservation = reservation;
        this.createStopEvent = createStopEvent;
    }

    @Override
    public Void call() throws Exception {

        if (createStopEvent) {
        final ReservationEndedEvent event = ReservationEndedEvent
                    .newBuilder()
                    .setSerializedKey(reservation.getSerializedKey())
                    .build();
            portalEventBus.post(event);
        }

        final ReservationClosedEvent event = ReservationClosedEvent.newBuilder().setSerializedKey(reservation.getSerializedKey()).build();
        portalEventBus.post(event);

        return null;
    }
}
