package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import eventstore.IEventStore;

public interface ReservationEventStore {

    public void reservationEnded(final ReservationEndedEvent event);

    public void reservationStarted(final ReservationStartedEvent event);

    public void stop();

    public IEventStore getEventStore();
}
