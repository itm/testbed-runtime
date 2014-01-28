package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.common.base.Function;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.shiro.dao.RoleDao;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

public class RoleResourceImpl implements RoleResource {

	private static final Logger log = LoggerFactory.getLogger(RoleResourceImpl.class);

	private final RoleDao rolesDao;

	@Inject
	public RoleResourceImpl(final RoleDao rolesDao) {
		this.rolesDao = rolesDao;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> listRoles() {
		log.trace("de.uniluebeck.itm.tr.snaa.shiro.rest.RoleResourceImpl.listRoles()");
		return newArrayList(transform(rolesDao.find(), new Function<Role, String>() {
			@Override
			public String apply(final Role role) {
				return role.getName();
			}
		}));
	}

}
