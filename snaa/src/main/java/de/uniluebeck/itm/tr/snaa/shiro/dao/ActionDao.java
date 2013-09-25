package de.uniluebeck.itm.tr.snaa.shiro.dao;

import de.uniluebeck.itm.tr.snaa.shiro.entity.Action;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;
import de.uniluebeck.itm.util.jpa.GenericDaoImpl;

/**
 * Instance of this class provide access to persisted users
 */
public class ActionDao extends GenericDaoImpl<Action, String> {

	public ActionDao(){
		super(Action.class);
	}

}
