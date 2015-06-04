package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.portal.EventBusFactory;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBusImpl;

public class FederatedReservationEventBus extends ReservationEventBusImpl {

	@Inject
	public FederatedReservationEventBus(final EventBusFactory eventBusFactory,
										@Assisted final Reservation reservation) {
		super(eventBusFactory, reservation);
	}

}
