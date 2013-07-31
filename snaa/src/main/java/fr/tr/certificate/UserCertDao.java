/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.tr.certificate;

import de.uniluebeck.itm.tr.snaa.shiro.entity.UsersCert;
import de.uniluebeck.itm.util.jpa.GenericDaoImpl;

/**
 *
 * 
 */
public class UserCertDao extends GenericDaoImpl<UsersCert, String> {
    
    public UserCertDao() {
        super(UsersCert.class);
    }
}
