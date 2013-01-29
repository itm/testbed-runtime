package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;

public interface DeliveryManagerFactory {

	DeliveryManager create(Reservation reservation);

}
