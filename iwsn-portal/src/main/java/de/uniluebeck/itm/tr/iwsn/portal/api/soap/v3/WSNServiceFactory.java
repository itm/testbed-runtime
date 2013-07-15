package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import eu.wisebed.api.v3.wsn.WSN;

public interface WSNServiceFactory {
	WSNService create(String reservationId, WSN wsn);
}
