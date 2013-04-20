package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import eu.wisebed.api.v3.rs.AuthorizationFault_Exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthorizationFaultExceptionMapper implements ExceptionMapper<AuthorizationFault_Exception> {

	@Override
	public Response toResponse(final AuthorizationFault_Exception exception) {
		return Response.status(Response.Status.UNAUTHORIZED).entity(exception.getMessage()).build();
	}
}