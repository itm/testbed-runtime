package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import de.uniluebeck.itm.tr.snaa.UserUnknownException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UserUnknownExceptionMapper implements ExceptionMapper<UserUnknownException> {

	@Override
	public Response toResponse(final UserUnknownException exception) {
		return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
	}
}