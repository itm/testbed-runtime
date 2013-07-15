package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import eu.wisebed.api.v3.snaa.SNAAFault_Exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SNAAFaultExceptionMapper implements ExceptionMapper<SNAAFault_Exception> {

	@Override
	public Response toResponse(final SNAAFault_Exception exception) {
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage())
				.build();
	}
}