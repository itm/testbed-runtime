package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.dao.RoleDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.UserDao;
import de.uniluebeck.itm.tr.snaa.shiro.dto.RoleDto;
import de.uniluebeck.itm.tr.snaa.shiro.dto.UserDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.RollbackException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.*;

public class UserResourceImpl implements UserResource {

	private static final Logger log = LoggerFactory.getLogger(UserResourceImpl.class);

	private final UserDao usersDao;

	private final RoleDao roleDao;

	private final String hashAlgorithmName;

	private final int hashIterations;

	@Inject
	public UserResourceImpl(final UserDao usersDao, final RoleDao roleDao,
							@Named("shiro.hashAlgorithmName") final String hashAlgorithmName,
							@Named("shiro.hashIterations") final int hashIterations) {
		this.usersDao = usersDao;
		this.roleDao = roleDao;
		this.hashAlgorithmName = hashAlgorithmName;
		this.hashIterations = hashIterations;
	}

	@Context
	private UriInfo uriInfo;

	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Transactional
	public Response addUser(final UserDto user) {

		if (usersDao.find(user.getName()) != null) {
			return Response
					.status(Response.Status.CONFLICT)
					.entity("User \"" + user.getName() + "\" already exists!\n")
					.build();
		}

		final Set<Role> requestedRoles = user.getRoles() != null && user.getRoles().size() > 0 ?
				newHashSet(transform(user.getRoles(), DTO_TO_ROLE_FUNCTION)) :
				Sets.<Role>newHashSet();
		final Set<Role> userRoles = newHashSet();

		for (Role requestedRole : requestedRoles) {
			Role userRole = roleDao.find(requestedRole.getName());
			if (userRole == null) {
				roleDao.save(requestedRole);
				userRole = requestedRole;
			}
			userRoles.add(userRole);
		}

		final String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
		final String hash = new SimpleHash(hashAlgorithmName, user.getPassword(), salt, hashIterations).toHex();
		final User newUser = new User(user.getName(), hash, salt, userRoles);

		try {
			usersDao.save(newUser);
		} catch (RollbackException e) {
			return Response.serverError().entity(e).build();
		}

		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(user.getName()).build();
		return Response.created(location).entity(convertUser(newUser)).build();
	}

	@Override
	@DELETE
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response deleteUser(@PathParam("name") final String name) {
		log.trace("UserResourceImpl.deleteUser({})", name);
		final User user = usersDao.find(name);
		if (user == null) {
			return Response.ok().build();
		}
		usersDao.delete(user);
		return Response.ok().build();
	}

	@Override
	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public UserDto getUser(@PathParam("name") final String name) {
		log.trace("UserResourceImpl.getUser()");
		return convertUser(usersDao.find(name));
	}

	@Override
	@PUT
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response updateUser(@PathParam("name") final String name, final UserDto userDto) {

		log.trace("UserResourceImpl.updateUser()");

		if (!userDto.getName().equals(name)) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("Username in request URL and request body do not match!")
					.build();
		}

		final User user = usersDao.find(name);
		if (user == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		boolean updated = false;

		// update password if it was changed ('null' password indicates no change)
		if (userDto.getPassword() != null) {
			final String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
			final String hash = new SimpleHash(hashAlgorithmName, userDto.getPassword(), salt, hashIterations).toHex();
			user.setPassword(hash);
			user.setSalt(salt);
			updated = true;
		}

		// update roles if they were updated
		final Set<RoleDto> dtoRoles = userDto.getRoles() == null ? Sets.<RoleDto>newHashSet() : userDto.getRoles();
		final Set<RoleDto> userRolesStrings = newHashSet(transform(user.getRoles(), ROLE_TO_DTO_FUNCTION));
		if (!dtoRoles.equals(userRolesStrings)) {

			// make sure requested roles exist
			final Set<Role> requestedRoles = newHashSet(transform(dtoRoles, DTO_TO_ROLE_FUNCTION));
			final Set<Role> userRoles = newHashSet();

			for (Role requestedRole : requestedRoles) {
				Role userRole = roleDao.find(requestedRole.getName());
				if (userRole == null) {
					roleDao.save(requestedRole);
					userRole = requestedRole;
				}
				userRoles.add(userRole);
			}

			user.setRoles(userRoles);
			updated = true;
		}

		// persist changes if any were made
		if (updated) {
			usersDao.update(user);
		}

		return Response.ok(convertUser(user)).build();
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public List<UserDto> listUsers() {
		log.trace("UserResourceImpl.listUsers(usersDao={})", usersDao);
		return convertUserList(usersDao.find());
	}
}
