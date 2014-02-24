package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.dto.UserDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.*;

public class UserResourceImpl implements UserResource {

	private static final Logger log = LoggerFactory.getLogger(UserResourceImpl.class);

	private final String hashAlgorithmName;

	private final int hashIterations;

	private final Provider<EntityManager> emProvider;

	@Context
	private UriInfo uriInfo;

	@Inject
	public UserResourceImpl(final Provider<EntityManager> emProvider,
							@Named("shiro.hashAlgorithmName") final String hashAlgorithmName,
							@Named("shiro.hashIterations") final int hashIterations) {
		this.emProvider = emProvider;
		this.hashAlgorithmName = hashAlgorithmName;
		this.hashIterations = hashIterations;
	}

	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response addUser(final UserDto user) {

		final EntityManager em = emProvider.get();

		if (em.find(User.class, user.getEmail()) != null) {
			return Response
					.status(Response.Status.CONFLICT)
					.entity("User \"" + user.getEmail() + "\" already exists!\n")
					.build();
		}

		final Set<Role> requestedRoles = user.getRoles() != null && user.getRoles().size() > 0 ?
				newHashSet(transform(user.getRoles(), DTO_TO_ROLE_FUNCTION)) :
				Sets.<Role>newHashSet();
		final Set<Role> userRoles = newHashSet();

		for (Role requestedRole : requestedRoles) {
			Role userRole = em.find(Role.class, requestedRole.getName());
			if (userRole == null) {
				em.persist(requestedRole);
				userRole = requestedRole;
			}
			userRoles.add(userRole);
		}

		final String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
		final String hash = new SimpleHash(hashAlgorithmName, user.getPassword(), salt, hashIterations).toHex();
		final User newUser = new User(user.getEmail(), hash, salt, userRoles);

		try {
			em.persist(newUser);
		} catch (RollbackException e) {
			return Response.serverError().entity(e).build();
		}

		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(user.getEmail()).build();
		final UserDto userDto = convertUser(newUser);

		return Response.created(location).entity(userDto).build();
	}

	@Override
	@DELETE
	@Path("{email}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response deleteUser(@PathParam("email") final String email) {
		log.trace("UserResourceImpl.deleteUser({})", email);
		final EntityManager em = emProvider.get();
		final User user = em.find(User.class, email);
		if (user == null) {
			return Response.ok().build();
		}
		for (Role role : user.getRoles()) {
			role.getUsers().remove(user);
		}
		em.remove(user);
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@Override
	@GET
	@Path("/{email}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public UserDto getUser(@PathParam("email") final String email) {
		log.trace("UserResourceImpl.getUser()");
		return convertUser(emProvider.get().find(User.class, email));
	}

	@Override
	@PUT
	@Path("/{email}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response updateUser(@PathParam("email") final String email, final UserDto userDto) {

		log.trace("UserResourceImpl.updateUser()");

		final EntityManager em = emProvider.get();

		if (!userDto.getEmail().equals(email)) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("User email in request URL and request body do not match!")
					.build();
		}

		final User user = em.find(User.class, email);
		if (user == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		// update password if it was changed ('null' password indicates no change)
		if (userDto.getPassword() != null) {
			final String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
			final String hash = new SimpleHash(hashAlgorithmName, userDto.getPassword(), salt, hashIterations).toHex();
			user.setPassword(hash);
			user.setSalt(salt);
		}

		final Set<Role> requestedRoles = newHashSet(transform(userDto.getRoles(), DTO_TO_ROLE_FUNCTION));
		final Set<Role> currentRoles = newHashSet(user.getRoles());
		final Set<Role> rolesRemovedFromUser = newHashSet(difference(currentRoles, requestedRoles));

		// remove user from roles that have been removed
		for (Role role : rolesRemovedFromUser) {
			role.getUsers().remove(user);
		}

		// make sure all roles exist and are part of current persistence context
		final Set<Role> requestedRolesToSet = newHashSet();
		for (Role requestedRole : requestedRoles) {
			final Role existingRole = em.find(Role.class, requestedRole.getName());
			if (existingRole == null) {
				em.persist(requestedRole);
				requestedRolesToSet.add(requestedRole);
			} else {
				requestedRolesToSet.add(existingRole);
			}
		}
		user.setRoles(requestedRolesToSet);

		return Response.ok(convertUser(user)).build();
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public List<UserDto> listUsers() {
		log.trace("UserResourceImpl.listUsers()");
		final EntityManager em = emProvider.get();
		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<User> query = criteriaBuilder.createQuery(User.class);
		query.from(User.class);
		final List<User> resultList = em.createQuery(query).getResultList();
		return convertUserList(resultList);
	}
}
