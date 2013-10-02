package de.uniluebeck.itm.tr.federator.iwsn;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;

public interface FederatedReservation extends Reservation {

	WSNFederatorService getWsnFederatorService();

	FederatorController getFederatorController();

}
