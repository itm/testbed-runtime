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

	@Subscribe
	public void on(final FederatedReservationScopedEvent scopedEvent) {

		log.trace("FederatedReservationEventBus.on({})", scopedEvent);

		if (scopedEvent.getReservation() == reservation) {

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
			} else if(event instanceof SingleNodeResponse) {
				onSingleNodeResponseFromPortalEventBus((SingleNodeResponse) event);
			} else if (event instanceof UpstreamMessageEvent) {
				onUpstreamMessageEventFromPortalEventBus((UpstreamMessageEvent) event);
			}
		}
	}
}
