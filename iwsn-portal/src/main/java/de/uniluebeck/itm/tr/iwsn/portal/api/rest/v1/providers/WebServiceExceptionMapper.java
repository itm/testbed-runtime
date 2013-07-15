package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.ws.WebServiceException;

@Provider
public class WebServiceExceptionMapper implements ExceptionMapper<WebServiceException> {

	@Override
	public Response toResponse(final WebServiceException exception) {
		return Response
				.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity("Some backend service (e.g., RS, SNAA) seems to be down. Exception message caught: " +
						exception.getMessage()
				).build();
	}
}