package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.portal.EventBusFactory;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBusImpl;

public class FederatedReservationEventBus extends ReservationEventBusImpl {

	@Inject
	public FederatedReservationEventBus(final PortalEventBus portalEventBus,
										final EventBusFactory eventBusFactory,
										@Assisted final Reservation reservation) {
		super(portalEventBus, eventBusFactory, reservation);
	}

}
