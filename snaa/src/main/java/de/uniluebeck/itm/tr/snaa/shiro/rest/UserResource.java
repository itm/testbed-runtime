package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.UserDto;
import de.uniluebeck.itm.tr.snaa.shiro.dto.UserListDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/users")
public interface UserResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
    UserListDto listUsers();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	Response addUser(final UserDto user);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    Response deleteUser(@PathParam("name") final String name);

}
