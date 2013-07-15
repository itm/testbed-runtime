package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class UnknownSecretReservationKeyException extends WebApplicationException {

	private static final long serialVersionUID = 6025480251388199505L;

	public UnknownSecretReservationKeyException(final String secretReservationKey) {
		super(Response
				.status(Response.Status.NOT_FOUND)
				.entity("Reservation with secret reservation key \""+secretReservationKey+"\" was not found!")
				.build()
		);
	}
}
