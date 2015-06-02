package de.uniluebeck.itm.tr.federator.iwsn;


import com.google.common.eventbus.Subscribe;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventDispatcherImpl;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FederatorReservationEventDispatcherImpl extends ReservationEventDispatcherImpl {
    private static final Logger log = LoggerFactory.getLogger(FederatorReservationEventDispatcherImpl.class);


    public FederatorReservationEventDispatcherImpl(PortalEventBus portalEventBus, ReservationManager reservationManager, PortalEventStore eventStore) {
        super(portalEventBus, reservationManager, eventStore, messageFactory);
    }

    @Subscribe
    public void on(final FederatedReservationScopedEvent scopedEvent) {

        log.trace("FederatorPortalEventDispatcherImpl.on({})", scopedEvent);

        final Object event = scopedEvent.getEvent();

        if (event instanceof DevicesAttachedEvent) {
            onDevicesAttachedEventFromPortalEventBus((DevicesAttachedEvent) event);
        } else if (event instanceof DevicesDetachedEvent) {
            onDevicesDetachedEventFromPortalEventBus((DevicesDetachedEvent) event);
        } else if (event instanceof GetChannelPipelinesResponse) {
            onGetChannelPipelinesResponse((GetChannelPipelinesResponse) event);
        } else if (event instanceof NotificationEvent) {
            onNotificationEventFromPortalEventBus((NotificationEvent) event);
        } else if (event instanceof ReservationEndedEvent) {
            onReservationEndedEventFromPortalEventBus((ReservationEndedEvent) event);
        } else if (event instanceof ReservationStartedEvent) {
            onReservationStartedEventFromPortalEventBus((ReservationStartedEvent) event);
        } else if (event instanceof SingleNodeProgress) {
            onSingleNodeProgressFromPortalEventBus((SingleNodeProgress) event);
        } else if (event instanceof SingleNodeResponse) {
            onSingleNodeResponseFromPortalEventBus((SingleNodeResponse) event);
        } else if (event instanceof UpstreamMessageEvent) {
            onUpstreamMessageEventFromPortalEventBus((UpstreamMessageEvent) event);
        }
    }
}

