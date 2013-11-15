package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import eu.wisebed.api.v3.wsn.WSN;

public interface WSNServiceFactory {
	WSNService create(Reservation reservation, WSN wsn);
}
