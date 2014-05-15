package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.IEventStore;

import java.io.FileNotFoundException;
import java.security.InvalidParameterException;

public interface PortalEventStoreHelper {

    IEventStore createAndConfigureEventStore(final String serializedReservationKey) throws FileNotFoundException;
    IEventStore createAndConfigureEventStore(final String serializedReservationKey, boolean readOnly) throws FileNotFoundException;

    String eventstoreBasenameForReservation(final String serializedReservationKey);

}
