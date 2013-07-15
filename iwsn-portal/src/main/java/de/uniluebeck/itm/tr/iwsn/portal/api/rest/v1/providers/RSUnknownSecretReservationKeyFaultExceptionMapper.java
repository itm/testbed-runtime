package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RSUnknownSecretReservationKeyFaultExceptionMapper
		implements ExceptionMapper<UnknownSecretReservationKeyFault> {

	@Override
	public Response toResponse(final UnknownSecretReservationKeyFault exception) {
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("Secret reservation key \"" + exception.getFaultInfo()
						.getSecretReservationKey() + "\" is unknown."
				).build();
	}
}