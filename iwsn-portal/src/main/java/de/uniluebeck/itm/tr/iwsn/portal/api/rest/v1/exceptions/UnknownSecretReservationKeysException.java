package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions;

import com.google.common.base.Joiner;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static de.uniluebeck.itm.tr.iwsn.portal.ReservationHelper.deserialize;

public class UnknownSecretReservationKeysException extends WebApplicationException {

	private static final long serialVersionUID = 6025480251388199505L;

	public UnknownSecretReservationKeysException(final String secretReservationKeysBase64) {
		super(Response
				.status(Response.Status.NOT_FOUND)
				.entity(
						"Reservation with secret reservation key \""
								+ Joiner.on(",").join(deserialize(secretReservationKeysBase64))
								+ "\" was not found!"
				)
				.build()
		);
	}
}
