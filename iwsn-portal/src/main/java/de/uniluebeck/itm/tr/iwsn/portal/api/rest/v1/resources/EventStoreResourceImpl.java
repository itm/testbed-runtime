package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStoreService;
import eventstore.IEventContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;

@Path("/events/")
public class EventStoreResourceImpl implements EventStoreResource {

	private final PortalEventStoreService eventStoreService;

	@Inject
	public EventStoreResourceImpl(final PortalEventStoreService eventStoreService) {
		this.eventStoreService = eventStoreService;
	}

	@Override
	@GET
	@Path("{secretReservationKeyBase64}")
	public Response getEvents(@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64) {

        try {
            Iterator<IEventContainer> iterator = eventStoreService.getEvents(secretReservationKeyBase64);
            while (iterator.hasNext()) {
                // TODO build json list from events
            }

            return null;
        } catch (IOException e) {
           return Response.status(Response.Status.BAD_REQUEST).entity("No events found for given reservation key!").build();
        }
	}
}
