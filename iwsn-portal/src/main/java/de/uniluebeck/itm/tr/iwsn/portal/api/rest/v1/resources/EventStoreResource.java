package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

public interface EventStoreResource {

	@GET
	@Path("{secretReservationKeyBase64}")
	Response getEvents(@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64);

}
