package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import eu.wisebed.api.v3.rs.ReservationConflictFault_Exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ReservationConflictFaultExceptionMapper
		implements ExceptionMapper<ReservationConflictFault_Exception> {

	@Override
	public Response toResponse(final ReservationConflictFault_Exception exception) {
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity(String.format("Another reservation is in conflict with yours: %s (%s)", exception,
						exception.getMessage()
				)
				)
				.build();
	}
}