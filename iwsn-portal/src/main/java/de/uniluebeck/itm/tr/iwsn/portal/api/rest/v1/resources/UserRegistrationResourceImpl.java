package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.UserRegistrationDto;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.UserAlreadyExistsException;
import de.uniluebeck.itm.tr.snaa.UserPwdMismatchException;
import de.uniluebeck.itm.tr.snaa.UserUnknownException;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;

@Path("/users/")
public class UserRegistrationResourceImpl implements UserRegistrationResource {

	private static final String USER_REGISTRATION_UNSUPPORTED_MSG =
			"User self-registration is disabled / not supported by this testbed. "
					+ "Please contact the testbed administrator to get an account!";

	private final SNAAService snaaService;

	@Context
	private UriInfo uriInfo;

	@Inject
	public UserRegistrationResourceImpl(final SNAAService snaaService) {
		this.snaaService = snaaService;
	}

	@Override
	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response add(final UserRegistrationDto userDto) throws UserAlreadyExistsException {

		if (!snaaService.isUserRegistrationSupported()) {
			return Response.status(Response.Status.FORBIDDEN).entity(USER_REGISTRATION_UNSUPPORTED_MSG).build();
		}

		try {
			snaaService.add(userDto.getEmail(), userDto.getPassword());
		} catch (IllegalArgumentException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}

		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(userDto.getEmail()).build();
		final UserRegistrationDto responseDto = new UserRegistrationDto();
		responseDto.setEmail(userDto.getEmail());
		return Response.created(location).entity(responseDto).build();
	}

	@Override
	@PUT
	@Path("{email}")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response update(@PathParam("email") final String email, final UserRegistrationDto userDto)
			throws UserPwdMismatchException, UserUnknownException {

		if (!snaaService.isUserRegistrationSupported()) {
			return Response.status(Response.Status.FORBIDDEN).entity(USER_REGISTRATION_UNSUPPORTED_MSG).build();
		}

		snaaService.update(email, userDto.getOldPassword(), userDto.getPassword());

		final UserRegistrationDto responseDto = new UserRegistrationDto();
		responseDto.setEmail(userDto.getEmail());
		return Response.ok(responseDto).build();
	}

	@Override
	@DELETE
	@Path("{email}")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response delete(@PathParam("email") final String email, final UserRegistrationDto userDto)
			throws UserPwdMismatchException, UserUnknownException {

		if (!snaaService.isUserRegistrationSupported()) {
			return Response.status(Response.Status.FORBIDDEN).entity(USER_REGISTRATION_UNSUPPORTED_MSG).build();
		}

		snaaService.delete(email, userDto.getPassword());
		return Response.noContent().build();
	}
}
