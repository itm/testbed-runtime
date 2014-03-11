package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface EventStoreResource {

    @GET
    @Path("{secretReservationKeyBase64}")
    @Produces("text/json")
    Response getEvents(@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64);

    @GET
    @Path("{secretReservationKeyBase64}")
    @Produces("text/json")
    Response getEventsBetweenTimestamps(@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64,
                                        @QueryParam("from") long fromTimestamp, @QueryParam("to") long toTimestamp);
}
