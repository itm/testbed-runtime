package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.*;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FederatedReservationEventBus extends ReservationEventBusImpl {

	private static final Logger log = LoggerFactory.getLogger(FederatedReservationEventBus.class);

	@Inject
	public FederatedReservationEventBus(final PortalEventBus portalEventBus,
										final EventBusFactory eventBusFactory,
										@Assisted final Reservation reservation) {
		super(portalEventBus, eventBusFactory, reservation);
	}

}
