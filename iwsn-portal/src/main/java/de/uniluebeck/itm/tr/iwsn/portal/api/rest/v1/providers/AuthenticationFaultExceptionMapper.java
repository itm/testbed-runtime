package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import eu.wisebed.api.v3.snaa.AuthenticationFault_Exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthenticationFaultExceptionMapper implements ExceptionMapper<AuthenticationFault_Exception> {

	@Override
	public Response toResponse(final AuthenticationFault_Exception exception) {
		String errorMessage = String.format("Authentication failed: %s (%s)", exception, exception.getMessage());
		return Response.status(Response.Status.FORBIDDEN).entity(errorMessage).build();
	}
}