package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.FlashProgramsRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

public interface RemoteExperimentConfigurationResource {

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	FlashProgramsRequest retrieveRemoteExperimentConfiguration(@QueryParam("url") String urlString);
}
