package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

public class ReservationEventDispatcherImpl extends AbstractService implements ReservationEventDispatcher {

	private static final Logger log = LoggerFactory.getLogger(ReservationEventDispatcherImpl.class);

	private final PortalEventBus portalEventBus;
	private final ReservationManager reservationManager;
	private final MessageFactory messageFactory;

	@Inject
	public ReservationEventDispatcherImpl(final PortalEventBus portalEventBus,
										  final ReservationManager reservationManager,
										  final MessageFactory messageFactory) {
		this.portalEventBus = checkNotNull(portalEventBus);
		this.reservationManager = checkNotNull(reservationManager);
		this.messageFactory = checkNotNull(messageFactory);
	}

	@Override
	protected void doStart() {
		log.trace("PortalEventDispatcherImpl.doStart()");
		try {
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onEvent(final Object obj) {

		if (MessageHeaderPair.isUnwrappedMessageEvent(obj)) {

			final MessageHeaderPair pair = MessageHeaderPair.fromUnwrapped(obj);

			if (pair.header.getUpstream()) {

				DateTime timestamp = new DateTime(pair.header.getTimestamp());

				if (pair.header.getBroadcast()) {

					reservationManager.getReservations(timestamp).forEach(res -> res.getEventBus().post(pair.message));

				} else {

					Multimap<Reservation, NodeUrn> mapping = reservationManager.getReservationMapping(
							newHashSet(transform(pair.header.getNodeUrnsList(), NodeUrn::new)),
							timestamp
					);

					mapping.keySet().forEach(res -> {

						MessageHeaderPair scopedEvent;
						if (MessageHeaderPair.isScopeableEvent(pair.header.getType())) {
							scopedEvent = messageFactory.scopeEvent(pair, mapping.get(res));
						} else {
							scopedEvent = pair;
						}

						res.getEventBus().post(scopedEvent.message);
					});
				}
			}
		}
	}
}
