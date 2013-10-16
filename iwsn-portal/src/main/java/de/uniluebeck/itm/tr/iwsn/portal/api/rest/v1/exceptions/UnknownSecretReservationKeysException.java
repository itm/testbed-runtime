package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions;

import com.google.common.base.Joiner;
import eu.wisebed.api.v3.common.SecretReservationKey;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;

public class UnknownSecretReservationKeysException extends WebApplicationException {

	private static final long serialVersionUID = 6025480251388199505L;

	public UnknownSecretReservationKeysException(final List<SecretReservationKey> secretReservationKeys) {
		super(Response
				.status(Response.Status.NOT_FOUND)
				.entity(
						"Reservation with secret reservation key \""
								+ Joiner.on(",").join(secretReservationKeys)
								+ "\" was not found!"
				)
				.build()
		);
	}
}
