package de.uniluebeck.itm.tr.snaa.shiro;

import de.uniluebeck.itm.tr.snaa.shiro.entity.User;

/**
 * Instance of this class provide access to persisted users
 */
public class UserDao extends GenericDaoImpl<User, String> {

	public UserDao(){
		super(User.class);
	}

}
