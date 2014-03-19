package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import eventstore.IEventStore;

import java.io.FileNotFoundException;

public interface PortalEventStoreHelper {

    IEventStore createAndConfigureEventStore(final String serializedReservationKey) throws FileNotFoundException;
    IEventStore createAndConfigureEventStore(final String serializedReservationKey, boolean readOnly) throws FileNotFoundException;

    String eventstoreBasenameForReservation(final String serializedReservationKey);

}
