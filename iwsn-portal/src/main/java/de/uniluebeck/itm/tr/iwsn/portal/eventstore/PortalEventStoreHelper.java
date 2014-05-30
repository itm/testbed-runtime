package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.IEventStore;

import java.io.FileNotFoundException;
import java.security.InvalidParameterException;

/**
 * The PortalEventStoreHelper is an abstraction layer to the file level when creating and loading event stores for reservations.
 */
public interface PortalEventStoreHelper {

    IEventStore createAndConfigureEventStore(final String serializedReservationKey) throws FileNotFoundException, ClassNotFoundException;


    /**
     * Loads the event store for the given reservation key if existing or throws an exception otherwise.
     *
     * @param serializedReservationKey the serialized key for the reservation to load the event store for.
     * @param readOnly
	 * @return the ReservationEventStore for the given serializedReservationKey. This event store is in read only mode, so write operations will fail.
     * @throws InvalidParameterException if there isn't an event store for the provided serializedReservationKey
     */
    IEventStore loadEventStore(final String serializedReservationKey, final boolean readOnly) throws InvalidParameterException;

    /**
     * Checks if an event store is existing for the given reservation key
     *
     * @param serializedReservationKey the reservation key to check for
     * @return <code>true</code> if an event store exists for the given key, <code>false</code> otherwise
     */

    boolean eventStoreExistsForReservation(final String serializedReservationKey);
}
