package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import de.uniluebeck.itm.tr.snaa.UserPwdMismatchException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UserPwdMismatchExceptionMapper implements ExceptionMapper<UserPwdMismatchException> {

	@Override
	public Response toResponse(final UserPwdMismatchException exception) {
		return Response.status(Response.Status.UNAUTHORIZED).entity(exception.getMessage()).build();
	}
}