package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import eu.wisebed.api.v3.rs.AuthorizationFault;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RSAuthorizationFaultExceptionMapper implements ExceptionMapper<AuthorizationFault> {

	@Override
	public Response toResponse(final AuthorizationFault exception) {
		return Response.status(Response.Status.UNAUTHORIZED).entity(exception.getMessage()).build();
	}
}