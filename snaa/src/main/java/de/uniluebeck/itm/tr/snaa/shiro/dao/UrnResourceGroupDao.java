package de.uniluebeck.itm.tr.snaa.shiro.dao;

import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroupId;
import de.uniluebeck.itm.util.jpa.GenericDaoImpl;


/**
 * Instance of this class provide access to persisted urn resource groups.
 */
public class UrnResourceGroupDao extends GenericDaoImpl<UrnResourceGroup, UrnResourceGroupId> {

	public UrnResourceGroupDao(){
		super(UrnResourceGroup.class);
	}

}
