package de.uniluebeck.itm.tr.devicedb;

import de.uniluebeck.itm.tr.common.dto.DeviceConfigDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/admin")
public interface DeviceDBRestAdminResource {

	@POST
	@Path("deviceConfigs/{nodeUrn}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response add(DeviceConfigDto deviceConfig, @PathParam("nodeUrn") String nodeUrnString);

	@PUT
	@Path("deviceConfigs/{nodeUrn}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response update(DeviceConfigDto deviceConfig, @PathParam("nodeUrn") String nodeUrnString);

	@DELETE
	@Path("deviceConfigs/{nodeUrn}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response delete(@PathParam("nodeUrn") String nodeUrnString);

	@DELETE
	@Path("deviceConfigs/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response delete(@QueryParam("nodeUrn") List<String> nodeUrnStrings);

}
