package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.PermissionDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/permissions")
public interface PermissionResource {

	@GET
    @Produces(MediaType.APPLICATION_JSON)
    List<PermissionDto> list();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	Response add(final PermissionDto permissionDto);

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	Response delete(@QueryParam("role") final String role,
					@QueryParam("action") final String action,
					@QueryParam("resourceGroup") final String resourceGroup);

}
