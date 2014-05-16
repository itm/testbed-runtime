package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationStartedEvent;
import de.uniluebeck.itm.eventstore.IEventStore;

public interface ReservationEventStore extends IEventStore, Service{

    public void reservationEnded(final ReservationEndedEvent event);

    public void reservationStarted(final ReservationStartedEvent event);
}
