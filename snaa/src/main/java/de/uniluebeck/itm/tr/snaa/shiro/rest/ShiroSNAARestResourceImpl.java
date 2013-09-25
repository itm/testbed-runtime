package de.uniluebeck.itm.tr.snaa.shiro.rest;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import de.uniluebeck.itm.tr.snaa.shiro.dao.ActionDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.RoleDao;
import de.uniluebeck.itm.tr.snaa.shiro.dao.UserDao;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Action;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Permission;
import de.uniluebeck.itm.tr.snaa.shiro.entity.PermissionId;
import de.uniluebeck.itm.tr.snaa.shiro.entity.ResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroupId;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;
import de.uniluebeck.itm.util.jpa.GenericDaoImpl;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.hibernate.annotations.MetaValue;
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

    @Path("list")
	public List<User> list() {
		
		Role r = new Role("Test");
		Set<Role> roles = new HashSet<Role>();
		roles.add(r);
		User newUser = new User("peter_p", "abc", "123", roles);
		
		//*
		try {
			entityManager.get().getTransaction().begin();
			entityManager.get().persist(newUser);
			entityManager.get().getTransaction().commit();
		} catch (EntityExistsException e) {
			log.trace("Entity exists...");
		}
		//*/

		/*
		try {
			entityManager.getTransaction().begin();
			entityManager.persist(r);
			entityManager.getTransaction().commit();
			
			usersDao.save(newUser);
		} catch (EntityExistsException e) {
			log.trace("Entity exists...");
		}
		/*/
		
		//User found = entityManager.find(User.class, "peter_pan");
		//return Lists.newArrayList(found);
		return usersDao.find();
	}
	
	@Override
	public Response addUser(String name, String password, String salt) {
		Role r = new Role("Test");
		Set<Role> roles = new HashSet<Role>();
		roles.add(r);
		User newUser = new User(name, password, salt, roles);
		usersDao.save(newUser);
		
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(name).build();
		return Response.created(location).entity("true").build();
	}

	@Override
	public Response test() {
        //*
		Role r = new Role("testRole1");
		//Action a = new Action("deleteAction");
		//Permission m = new Permission(new PermissionId("testRole", "deleteAction"), a, r);
		//ResourceGroup rg = new ResourceGroup("testGroup", urnResourceGroups, Sets.newHashSet(m));
		//UrnResourceGroup urg = new UrnResourceGroup(new UrnResourceGroupId("uzl", "Uni"), rg);
		Set<Role> roles = new HashSet<Role>();
		roles.add(r);
		User newUser = new User("peter_pan3", "abc", "123", roles);
		usersDao.save(newUser);
		
		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment("peter_pan3").build();
		return Response.created(location).entity("true").build();
	}

	@Override
	public List<User> listUsers() {
		return usersDao.find();
	}

	@Override
	public List<Role> listRoles() {
		return rolesDao.find();
	}

	@Override
	public List<Action> listActions() {
		return actionsDao.find();
	}

}
