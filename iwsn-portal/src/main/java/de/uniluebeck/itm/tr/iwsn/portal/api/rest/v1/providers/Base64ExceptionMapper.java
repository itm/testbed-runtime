package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import org.apache.cxf.common.util.Base64Exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class Base64ExceptionMapper implements ExceptionMapper<Base64Exception> {

	@Override
	public Response toResponse(final Base64Exception exception) {
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("Request URL or payload contains data that is not correctly (or not) Base64-encoded. Error message: " +
						exception.getMessage()
				).build();
	}
}