package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class BadRemoteExperimentConfiguration extends WebApplicationException {
	private static final long serialVersionUID = 6025480251388199505L;

	public BadRemoteExperimentConfiguration(String reason, Exception cause) {
		super(Response.status(Response.Status.BAD_REQUEST).entity(reason + "(" + cause + ")").build());
	}
}
