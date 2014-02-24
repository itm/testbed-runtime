package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import de.uniluebeck.itm.tr.snaa.UserAlreadyExistsException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UserAlreadyExistsExceptionMapper implements ExceptionMapper<UserAlreadyExistsException> {

	@Override
	public Response toResponse(final UserAlreadyExistsException exception) {
		return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
	}
}