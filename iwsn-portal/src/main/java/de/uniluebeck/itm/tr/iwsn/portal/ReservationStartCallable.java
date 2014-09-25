package de.uniluebeck.itm.tr.iwsn.portal;

import de.uniluebeck.itm.tr.iwsn.messages.ReservationOpenedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;

import java.util.concurrent.Callable;

public class ReservationStartCallable implements Callable<Void> {

    private final PortalEventBus portalEventBus;

    private final Reservation reservation;
    private final boolean createStatedEvent;

    public ReservationStartCallable(final PortalEventBus portalEventBus, final Reservation reservation, final boolean createStatedEvent) {
        this.portalEventBus = portalEventBus;
        this.reservation = reservation;
        this.createStatedEvent = createStatedEvent;
    }

    @Override
    public Void call() throws Exception {
        reservation.startAndWait();
        if (createStatedEvent) {
            final ReservationStartedEvent event = ReservationStartedEvent
                    .newBuilder()
                    .setSerializedKey(reservation.getSerializedKey())
                    .build();
            portalEventBus.post(event);
        }

        final ReservationOpenedEvent event = ReservationOpenedEvent.newBuilder().setSerializedKey(reservation.getSerializedKey()).build();
        portalEventBus.post(event);
        return null;
    }
}
