package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.dto.RoleDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.ROLE_TO_DTO_FUNCTION;

public class RoleResourceImpl implements RoleResource {

	private static final Logger log = LoggerFactory.getLogger(RoleResourceImpl.class);

	private final Provider<EntityManager> emProvider;

	@Context
	private UriInfo uriInfo;

	@Inject
	public RoleResourceImpl(final Provider<EntityManager> emProvider) {
		this.emProvider = emProvider;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public List<RoleDto> listRoles() {
		log.trace("RoleResourceImpl.listRoles()");
		final EntityManager em = emProvider.get();
		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<Role> query = criteriaBuilder.createQuery(Role.class);
		query.from(Role.class);
		return newArrayList(transform(em.createQuery(query).getResultList(), ROLE_TO_DTO_FUNCTION));
	}

	@Override
	@POST
	@Transactional
	public Response addRole(final RoleDto role) {
		log.trace("RoleResourceImpl.addRole()");
		final EntityManager em = emProvider.get();
		if (em.find(Role.class, role.getName()) != null) {
			return Response.ok().build();
		}
		em.persist(new Role(role.getName()));
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(role.getName()).build();
		return Response.created(location).entity(role).build();
	}

	@Override
	@DELETE
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response removeRole(@PathParam("name") final String role) {

		log.trace("RoleResourceImpl.removeRole()");

		final EntityManager em = emProvider.get();
		final Role roleFound = em.find(Role.class, role);

		if (roleFound == null) {
			return Response.ok().build();
		}

		if (roleFound.getUsers().size() > 0) {
			return Response
					.status(Response.Status.FORBIDDEN)
					.entity("The role \"" + role + "\" is still assigned to users and can therefore not be deleted.")
					.build();
		}

		em.remove(roleFound);

		return Response.status(Response.Status.NO_CONTENT).build();
	}

}
