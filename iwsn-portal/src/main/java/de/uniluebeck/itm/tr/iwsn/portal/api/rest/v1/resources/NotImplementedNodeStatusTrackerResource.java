package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import javax.ws.rs.core.Response;

public class NotImplementedNodeStatusTrackerResource implements NodeStatusTrackerResource {

	@Override
	public Response getNodeStatusList() {
		return Response
				.status(Response.Status.NOT_IMPLEMENTED)
				.entity("NotStatusTrackerResource not yet implemented for Federator!")
				.build();
	}
}
