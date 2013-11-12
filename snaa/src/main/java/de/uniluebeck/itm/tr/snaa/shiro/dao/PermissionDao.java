package de.uniluebeck.itm.tr.snaa.shiro.dao;

import de.uniluebeck.itm.tr.snaa.shiro.entity.Permission;
import de.uniluebeck.itm.util.jpa.GenericDaoImpl;

public class PermissionDao extends GenericDaoImpl<Permission, String> {

    public PermissionDao() {
        super(Permission.class);
    }
}
