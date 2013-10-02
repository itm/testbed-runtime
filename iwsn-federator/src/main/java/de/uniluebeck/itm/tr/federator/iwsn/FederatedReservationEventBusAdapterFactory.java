package de.uniluebeck.itm.tr.federator.iwsn;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;

public interface FederatedReservationEventBusAdapterFactory {

	FederatedReservationEventBusAdapter create(Reservation reservation,
											   ReservationEventBus reservationEventBus,
											   FederatorController federatorController,
											   WSNFederatorService wsnFederatorService);
}
