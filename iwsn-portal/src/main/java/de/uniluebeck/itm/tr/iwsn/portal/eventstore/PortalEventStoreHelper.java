package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import eventstore.IEventStore;

import java.io.FileNotFoundException;

public interface PortalEventStoreHelper {

    IEventStore createAndConfigureEventStore(String serializedReservationKey) throws FileNotFoundException;

    String suggestedEventStoreBaseNameForReservation(String serializedReservationKey);
}
