package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.UserRegistrationDto;
import de.uniluebeck.itm.tr.snaa.UserAlreadyExistsException;
import de.uniluebeck.itm.tr.snaa.UserPwdMismatchException;
import de.uniluebeck.itm.tr.snaa.UserUnknownException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/users/")
public interface UserRegistrationResource {

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response add(UserRegistrationDto userDto) throws UserAlreadyExistsException;

	@PUT
	@Path("{email}")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response update(@PathParam("email") String email, UserRegistrationDto userDto)
			throws UserPwdMismatchException, UserUnknownException;

	@DELETE
	@Path("{email}")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response delete(@PathParam("email") String email, UserRegistrationDto userDto)
			throws UserPwdMismatchException, UserUnknownException;

}
