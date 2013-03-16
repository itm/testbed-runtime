package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import eu.wisebed.api.v3.wsn.WSN;

public interface WSNFactory {
	WSN create(String reservationId, Reservation reservation, final DeliveryManager deliveryManager);
}

