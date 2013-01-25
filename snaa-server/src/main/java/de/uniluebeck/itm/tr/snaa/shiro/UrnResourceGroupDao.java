package de.uniluebeck.itm.tr.snaa.shiro;

import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroupId;


/**
 * Instance of this class provide access to persisted urn resource groups.
 */
public class UrnResourceGroupDao extends GenericDaoImpl<UrnResourceGroup, UrnResourceGroupId>{

	public UrnResourceGroupDao(){
		super(UrnResourceGroup.class);
	}

}
