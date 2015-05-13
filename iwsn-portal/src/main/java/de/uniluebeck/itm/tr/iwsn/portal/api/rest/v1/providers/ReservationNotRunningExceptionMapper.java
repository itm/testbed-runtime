package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.ReservationNotRunningException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ReservationNotRunningExceptionMapper implements ExceptionMapper<ReservationNotRunningException> {

    @Override
    public Response toResponse(final ReservationNotRunningException exception) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(exception.getMessage()).build();
    }
}
