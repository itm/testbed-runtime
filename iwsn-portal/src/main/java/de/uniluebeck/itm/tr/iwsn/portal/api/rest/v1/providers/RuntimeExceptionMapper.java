package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import com.google.common.base.Throwables;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

	@Override
	public Response toResponse(final RuntimeException exception) {
		return Response
				.serverError()
				.entity(exception.getMessage() + "\n" + Throwables.getStackTraceAsString(exception))
				.build();
	}
}