package de.uniluebeck.itm.tr.snaa.shiro.rest;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

import de.uniluebeck.itm.tr.snaa.shiro.dao.ActionDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.RoleDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.UserDao;
import de.uniluebeck.itm.tr.snaa.shiro.dto.*;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;

import javax.persistence.EntityManager;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShiroSNAARestResourceImpl implements ShiroSNAARestResource {

	private static final Logger log = LoggerFactory.getLogger(ShiroSNAARestResourceImpl.class);
	private final Provider<EntityManager> entityManager;

	private final UserDao usersDao;
	private final ActionDao actionsDao;
	private final RoleDao rolesDao;

	@Inject
	public ShiroSNAARestResourceImpl(	final Provider<EntityManager> entityManager,
										final UserDao usersDao,
										final ActionDao actionsDao,
										final RoleDao rolesDao) {
		this.entityManager = entityManager;
		this.usersDao = usersDao;
		this.actionsDao = actionsDao;
		this.rolesDao = rolesDao;
	}

	@Context
	private UriInfo uriInfo;
	
	@Override
	public Response addUser(String name, String password, String salt) {
        Set<Role> roles = Sets.newHashSet();
        User newUser = new User(name, password, salt, roles);
		usersDao.save(newUser);
		
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(name).build();
		return Response.created(location).entity("true").build();
	}

	@Override
	public UserListDto listUsers() {
		return ShiroEntityDaoConverter.userList(usersDao.find());
	}


}
