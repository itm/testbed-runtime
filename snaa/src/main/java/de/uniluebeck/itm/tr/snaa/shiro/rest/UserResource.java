package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.UserDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/users")
public interface UserResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	List<UserDto> listUsers();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	Response addUser(final UserDto user);

	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	UserDto getUser(@PathParam("name") final String name);

	@PUT
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	Response updateUser(@PathParam("name") final UserDto user);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    Response deleteUser(@PathParam("name") final String name);

}
