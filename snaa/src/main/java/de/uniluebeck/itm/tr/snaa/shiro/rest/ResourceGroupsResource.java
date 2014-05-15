package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.ResourceGroupDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/resource_groups")
public interface ResourceGroupsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ResourceGroupDto> list();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	Response add(final ResourceGroupDto resourceGroup);

	@PUT
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	Response update(@PathParam("name") final String name, final ResourceGroupDto resourceGroup);

	@DELETE
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	Response delete(@PathParam("name") final String name);

}
