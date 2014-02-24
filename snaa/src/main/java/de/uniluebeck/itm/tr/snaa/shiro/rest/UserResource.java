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
	@Path("/{email}")
	@Produces(MediaType.APPLICATION_JSON)
	UserDto getUser(@PathParam("email") final String email);

	@PUT
	@Path("/{email}")
	@Produces(MediaType.APPLICATION_JSON)
	Response updateUser(@PathParam("email") final String email, final UserDto user);

    @DELETE
    @Path("/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    Response deleteUser(@PathParam("email") final String email);

}
