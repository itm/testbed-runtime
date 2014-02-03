package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.shiro.dao.RoleDao;
import de.uniluebeck.itm.tr.snaa.shiro.dto.RoleDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Context
	private UriInfo uriInfo;

	@Inject
	public RoleResourceImpl(final RoleDao rolesDao) {
		this.rolesDao = rolesDao;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<RoleDto> listRoles() {
		log.trace("RoleResourceImpl.listRoles()");
		return newArrayList(transform(rolesDao.find(), ROLE_TO_DTO_FUNCTION));
	}

	@Override
	@POST
	public Response addRole(final RoleDto role) {
		log.trace("RoleResourceImpl.addRole()");
		if (rolesDao.find(role.getName()) != null) {
			return Response.ok().build();
		}
		rolesDao.save(new Role(role.getName()));
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(role.getName()).build();
		return Response.created(location).build();
	}

	@Override
	@DELETE
	public Response removeRole(final RoleDto role) {
		log.trace("RoleResourceImpl.removeRole()");
		final Role roleFound = rolesDao.find(role.getName());
		if (roleFound == null) {
			return Response.ok().build();
		}
		rolesDao.delete(roleFound);
		return Response.status(Response.Status.NO_CONTENT).build();
	}

}
