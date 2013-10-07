package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.UserDto;
import de.uniluebeck.itm.tr.snaa.shiro.dto.UserListDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public interface ShiroSNAARestResource {

	@GET
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
    UserListDto listUsers();

	@POST
	@Path("users")
	@Consumes(MediaType.APPLICATION_JSON)
	Response addUser(UserDto user);

    @DELETE
    @Path("users/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    Response deleteUser(@PathParam("name") final String name);

}
