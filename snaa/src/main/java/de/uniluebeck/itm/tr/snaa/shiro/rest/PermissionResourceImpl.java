package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.dto.PermissionDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.PERMISSION_TO_DTO_FUNCTION;
import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.convertPermissionList;

public class PermissionResourceImpl implements PermissionResource {

	private static final Logger log = LoggerFactory.getLogger(PermissionResourceImpl.class);

	private final Provider<EntityManager> emProvider;

	@Inject
	public PermissionResourceImpl(final Provider<EntityManager> emProvider) {
		this.emProvider = emProvider;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public List<PermissionDto> list() {
		log.trace("UserResourceImpl.listPermissions()");
		final EntityManager em = emProvider.get();
		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<Permission> query = criteriaBuilder.createQuery(Permission.class);
		query.from(Permission.class);
		return convertPermissionList(em.createQuery(query).getResultList());
	}

	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Transactional
	public Response add(final PermissionDto dto) {

		log.trace("PermissionResourceImpl.add({})", dto);

		if (loadPermission(dto) != null) {
			return Response
					.status(Response.Status.CONFLICT)
					.entity("Permission already exists!\n")
					.build();
		}

		final EntityManager em = emProvider.get();

		final Action action = em.find(Action.class, dto.getActionName());
		if (action == null) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("An action named \"" + dto.getActionName() + "\" does not exist!\n")
					.build();
		}

		final ResourceGroup resourceGroup = em.find(ResourceGroup.class, dto.getResourceGroupName());
		if (resourceGroup == null) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("A resource group named \"" + dto.getResourceGroupName() + "\" does not exist!\n")
					.build();
		}

		final Role role = em.find(Role.class, dto.getRoleName());
		if (role == null) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("A role named \"" + dto.getRoleName() + "\" does not exist!\n")
					.build();
		}

		final PermissionId id = new PermissionId(dto.getRoleName(), dto.getActionName(), dto.getResourceGroupName());
		final Permission permission = new Permission(id, action, resourceGroup, role);

		em.persist(permission);

		return Response.ok(PERMISSION_TO_DTO_FUNCTION.apply(permission)).build();
	}

	@Override
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response delete(@QueryParam("role") final String role,
						   @QueryParam("action") final String action,
						   @QueryParam("resourceGroup") final String resourceGroup) {
		log.trace("PermissionResourceImpl.delete(role={},action={},resourceGroup={})", role, action, resourceGroup);
		final Permission permission = loadPermission(role, action, resourceGroup);
		if (permission != null) {
			permission.getAction().getPermissions().remove(permission);
			permission.getResourceGroup().getPermissions().remove(permission);
			permission.getAction().getPermissions().remove(permission);
			emProvider.get().remove(permission);
		}
		return Response.noContent().build();
	}

	@Nullable
	private Permission loadPermission(final String role, final String action, final String resourceGroup) {

		final EntityManager em = emProvider.get();
		final Query query = em.createQuery(
				"SELECT p FROM Permission p WHERE p.id.roleName=:roleName AND p.id.actionName=:actionName AND p.id.resourcegroupName=:resourcegroupName"
		);

		query.setParameter("roleName", role);
		query.setParameter("actionName", action);
		query.setParameter("resourcegroupName", resourceGroup);

		final List resultList = query.getResultList();

		return resultList.size() == 0 ? null : (Permission) resultList.get(0);
	}

	@Nullable
	private Permission loadPermission(final PermissionDto dto) {
		return loadPermission(dto.getRoleName(), dto.getActionName(), dto.getResourceGroupName());
	}
}
