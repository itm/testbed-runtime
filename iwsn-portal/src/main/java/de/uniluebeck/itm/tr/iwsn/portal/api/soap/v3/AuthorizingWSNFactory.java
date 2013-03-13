package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import eu.wisebed.api.v3.wsn.WSN;

/**
 * Implementations of this interface create WSN instances which
 * check whether an action initiated by a user is allowed due to
 * the users access rights.
 */
public interface AuthorizingWSNFactory {
	public AuthorizingWSN create(Reservation reservation, WSN delegate);
}
