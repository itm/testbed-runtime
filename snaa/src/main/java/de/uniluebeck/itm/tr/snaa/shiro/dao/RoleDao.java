package de.uniluebeck.itm.tr.snaa.shiro.dao;

import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;
import de.uniluebeck.itm.util.jpa.GenericDaoImpl;

/**
 * Instance of this class provide access to persisted users
 */
public class RoleDao extends GenericDaoImpl<Role, String> {

	public RoleDao(){
		super(Role.class);
	}

}
