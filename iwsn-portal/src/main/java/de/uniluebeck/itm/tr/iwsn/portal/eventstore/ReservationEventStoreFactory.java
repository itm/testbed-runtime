package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;

public interface ReservationEventStoreFactory {

    ReservationEventStore createOrLoad(@Assisted final Reservation reservation);

}
