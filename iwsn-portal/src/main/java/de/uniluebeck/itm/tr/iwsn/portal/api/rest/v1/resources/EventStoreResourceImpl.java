package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventStoreService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

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

		return Response.noContent().build();
	}
}
