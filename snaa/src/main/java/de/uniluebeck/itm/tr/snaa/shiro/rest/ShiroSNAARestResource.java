package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/")
public interface ShiroSNAARestResource {

	@GET
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
    UserListDto listUsers();

	@POST
	@Path("users")
	@Consumes(MediaType.APPLICATION_JSON)
	Response addUser(final UserDto user);

    @DELETE
    @Path("users/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    Response deleteUser(@PathParam("name") final String name);

    @GET
    @Path("actions")
    @Produces(MediaType.APPLICATION_JSON)
    List<ActionDto> listActions();

    @GET
    @Path("permissions")
    @Produces(MediaType.APPLICATION_JSON)
    List<PermissionDto> listPermissions();

    @GET
    @Path("roles")
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleDto> listRoles();

}
