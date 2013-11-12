package de.uniluebeck.itm.tr.snaa.shiro.rest;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

import de.uniluebeck.itm.tr.snaa.shiro.dao.ActionDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.PermissionDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.RoleDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.UserDao;
import de.uniluebeck.itm.tr.snaa.shiro.dto.*;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShiroSNAARestResourceImpl implements ShiroSNAARestResource {

	private static final Logger log = LoggerFactory.getLogger(ShiroSNAARestResourceImpl.class);
	private final Provider<EntityManager> entityManager;

	private final UserDao usersDao;
	private final ActionDao actionsDao;
	private final RoleDao rolesDao;
    private final PermissionDao permissionsDao;

	@Inject
	public ShiroSNAARestResourceImpl(	final Provider<EntityManager> entityManager,
										final UserDao usersDao,
										final ActionDao actionsDao,
										final RoleDao rolesDao,
                                        final PermissionDao permissionsDao) {
		this.entityManager = entityManager;
		this.usersDao = usersDao;
		this.actionsDao = actionsDao;
		this.rolesDao = rolesDao;
        this.permissionsDao = permissionsDao;
	}

	@Context
	private UriInfo uriInfo;
	
	@Override
	public Response addUser(final UserDto user) {
        Set<Role> roles = Sets.newHashSet();
        //TODO salt, roles
        User newUser = new User(user.getName(), user.getPassword(), "", roles);
		try {
            usersDao.save(newUser);

        } catch (RollbackException e) {
            return Response.serverError().entity(e.getLocalizedMessage()).build();
        }

		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(user.getName()).build();
		return Response.created(location).entity("true").build();
	}

    @Override
    public Response deleteUser(@PathParam("name") final String name) {
        //TODO delete Path doesn't get found
        log.trace("ShiroSNAARestResourceImpl.deleteUser({})", name);
        boolean deleted = false;
        try {
            User user = usersDao.find(name);
            usersDao.delete(user);
            deleted = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return deleted ? Response.ok().build() : Response.notModified().build();
    }

    @Override
	public UserListDto listUsers() {
        log.trace("ShiroSNAARestResourceImpl.listUsers()");
		return ShiroEntityDaoConverter.userList(usersDao.find());
	}

    @Override
    public List<ActionDto> listActions() {
        log.trace("ShiroSNAARestResourceImpl.listActions()");
        return ShiroEntityDaoConverter.toActionDtoList(actionsDao.find());
    }

    @Override
    public List<PermissionDto> listPermissions() {
        log.trace("ShiroSNAARestResourceImpl.listPermissions()");
        return ShiroEntityDaoConverter.toPermissionDtoList(permissionsDao.find());
    }

    @Override
    public List<RoleDto> listRoles() {
        return null;
    }

}
