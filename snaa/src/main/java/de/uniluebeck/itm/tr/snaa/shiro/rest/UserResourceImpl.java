package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.common.base.Function;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.shiro.dao.RoleDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.UserDao;
import de.uniluebeck.itm.tr.snaa.shiro.dto.ShiroEntityDaoConverter;
import de.uniluebeck.itm.tr.snaa.shiro.dto.UserDto;
import de.uniluebeck.itm.tr.snaa.shiro.dto.UserListDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;
import edu.vt.middleware.crypt.digest.SHA512;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.RollbackException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

public class UserResourceImpl implements UserResource {

	private static final Logger log = LoggerFactory.getLogger(UserResourceImpl.class);

	private static final Function<String, Role> STRING_TO_ROLE_FUNCTION = new Function<String, Role>() {
		@Override
		public Role apply(final String s) {
			return new Role(s);
		}
	};

	private final UserDao usersDao;

	private final RoleDao roleDao;

	@Inject
	public UserResourceImpl(final UserDao usersDao, final RoleDao roleDao) {
		this.usersDao = usersDao;
		this.roleDao = roleDao;
	}

	@Context
	private UriInfo uriInfo;

	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addUser(final UserDto user) {

		if (usersDao.find(user.getName()) != null) {
			return Response
					.status(Response.Status.CONFLICT)
					.entity("User \"" + user.getName() + "\" already exists!\n")
					.build();
		}

		final Set<Role> requestedRoles = newHashSet(transform(user.getRoles(), STRING_TO_ROLE_FUNCTION));
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
		final String hash = new SimpleHash(SHA512.ALGORITHM, user.getPassword(), salt, 1000).toHex();
		final User newUser = new User(user.getName(), hash, salt, userRoles);

		try {
			usersDao.save(newUser);
		} catch (RollbackException e) {
			return Response.serverError().entity(e).build();
		}

		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(user.getName()).build();
		return Response.created(location).build();
	}

	@Override
	@DELETE
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
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
	@Produces(MediaType.APPLICATION_JSON)
	public UserListDto listUsers() {
		log.trace("UserResourceImpl.listUsers()");
		return ShiroEntityDaoConverter.userList(usersDao.find());
	}

}
