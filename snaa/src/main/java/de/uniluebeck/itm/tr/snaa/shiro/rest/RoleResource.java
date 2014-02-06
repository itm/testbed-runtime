package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.RoleDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/roles")
public interface RoleResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleDto> listRoles();

	@POST
	Response addRole(RoleDto role);

	@DELETE
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	Response removeRole(@PathParam("name") String role);

}
