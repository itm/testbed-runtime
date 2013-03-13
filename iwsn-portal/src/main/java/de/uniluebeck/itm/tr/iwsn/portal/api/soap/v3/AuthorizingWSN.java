package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import eu.wisebed.api.v3.wsn.WSN;

/**
 * Implementations of this interface check whether an action
 * initiated by a user is allowed due to the users access rights.
 * If the action is authorized successfully, the action is
 * actually performed.
 */
public interface AuthorizingWSN extends WSN {}
