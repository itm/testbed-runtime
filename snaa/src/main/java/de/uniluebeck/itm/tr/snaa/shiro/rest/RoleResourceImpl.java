package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.snaa.shiro.dao.RoleDao;
import de.uniluebeck.itm.tr.snaa.shiro.dto.RoleDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.ROLE_TO_DTO_FUNCTION;

public class RoleResourceImpl implements RoleResource {

	private static final Logger log = LoggerFactory.getLogger(RoleResourceImpl.class);

	private final RoleDao rolesDao;

	private final Provider<EntityManager> em;

	@Context
	private UriInfo uriInfo;

	@Inject
	public RoleResourceImpl(final RoleDao rolesDao, final Provider<EntityManager> em) {
		this.rolesDao = rolesDao;
		this.em = em;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<RoleDto> listRoles() {
		log.trace("RoleResourceImpl.listRoles()");
		final EntityTransaction transaction = em.get().getTransaction();
		transaction.begin();
		final List<Role> roles = rolesDao.find();
		transaction.commit();
		return newArrayList(transform(roles, ROLE_TO_DTO_FUNCTION));
	}

	@Override
	@POST
	public Response addRole(final RoleDto role) {
		log.trace("RoleResourceImpl.addRole()");
		final EntityTransaction transaction = em.get().getTransaction();
		transaction.begin();
		if (rolesDao.find(role.getName()) != null) {
			return Response.ok().build();
		}
		rolesDao.save(new Role(role.getName()));
		transaction.commit();
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(role.getName()).build();
		return Response.created(location).entity(role).build();
	}

	@Override
	@DELETE
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeRole(@PathParam("name") final String role) {
		log.trace("RoleResourceImpl.removeRole()");
		final EntityTransaction transaction = em.get().getTransaction();
		transaction.begin();
		final Role roleFound = rolesDao.find(role);
		if (roleFound == null) {
			return Response.ok().build();
		}
		rolesDao.delete(roleFound);
		transaction.commit();
		return Response.status(Response.Status.NO_CONTENT).build();
	}

}
