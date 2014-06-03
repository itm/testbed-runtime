package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import javax.ws.rs.core.Response;

public class NotImplementedEventStoreResource implements EventStoreResource {

	@Override
	public Response getEvents(final String secretReservationKeyBase64, final long fromTimestamp,
							  final long toTimestamp) {
		return Response.ok().build();
	}

}
