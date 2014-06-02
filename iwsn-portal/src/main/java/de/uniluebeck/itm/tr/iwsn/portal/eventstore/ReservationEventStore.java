package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;

public interface ReservationEventStore extends IEventStore, Service{

	public void reservationStarted(final ReservationStartedEvent event);

    public void reservationEnded(final ReservationEndedEvent event);
}
