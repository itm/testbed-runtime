package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class NotLoggedInException extends WebApplicationException {
	private static final long serialVersionUID = 6025480251388199505L;

	public NotLoggedInException() {
		super(Response
				.status(Response.Status.FORBIDDEN)
				.entity("You're not logged in into testbed!")
				.build()
		);
	}
}
