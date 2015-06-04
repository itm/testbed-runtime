package de.uniluebeck.itm.tr.federator.iwsn;


import com.google.common.eventbus.Subscribe;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventDispatcherImpl;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class FederatorReservationEventDispatcherImpl extends ReservationEventDispatcherImpl {

    private static final Logger log = LoggerFactory.getLogger(FederatorReservationEventDispatcherImpl.class);


	@Inject
    public FederatorReservationEventDispatcherImpl(final PortalEventBus portalEventBus,
												   final ReservationManager reservationManager,
												   final MessageFactory messageFactory) {
        super(portalEventBus, reservationManager, messageFactory);
    }

    @Subscribe
    public void on(final FederatedReservationScopedEvent scopedEvent) {
        log.trace("FederatorPortalEventDispatcherImpl.on({})", scopedEvent);
        super.onEvent(scopedEvent.getEvent());
    }
}

