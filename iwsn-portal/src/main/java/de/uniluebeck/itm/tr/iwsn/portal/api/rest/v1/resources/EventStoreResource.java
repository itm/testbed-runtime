package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface EventStoreResource {

    @GET
    @Path("{secretReservationKeyBase64}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getEvents(@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64,
                       @QueryParam("from") long fromTimestamp, @QueryParam("to") long toTimestamp);
}
