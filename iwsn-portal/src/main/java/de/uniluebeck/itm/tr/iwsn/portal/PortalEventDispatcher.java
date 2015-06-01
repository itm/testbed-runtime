package de.uniluebeck.itm.tr.iwsn.portal;


import com.google.common.util.concurrent.Service;

/**
 * The PortalEventDispatcher subscribes to the PortalEventBus and dispatches upstream message posts on the bus to the
 * ReservationEventBus instances of the individual reservations. If an event covers nodes from multiple reservations or
 * contains nodes that are not part of a reservation it will split up the event so that each reservation only sees
 * events for the reserved nodes.
 */
public interface PortalEventDispatcher extends Service {

}
