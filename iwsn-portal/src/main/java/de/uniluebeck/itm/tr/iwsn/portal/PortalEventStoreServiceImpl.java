package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalEventStoreServiceImpl extends AbstractService implements PortalEventStoreService {

	private static final Logger log = LoggerFactory.getLogger(PortalEventStoreServiceImpl.class);

	private final PortalEventBus portalEventBus;

	@Inject
	public PortalEventStoreServiceImpl(final PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Override
	protected void doStart() {
		log.trace("PortalEventStoreServiceImpl.doStart()");
		try {
			// TODO implement
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("PortalEventStoreServiceImpl.doStop()");
		try {
			// TODO implement
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onReservationStarted(final ReservationStartedEvent event) {
		log.trace("PortalEventStoreServiceImpl.onReservationStarted()"); // TODO remove when working
		final ReservationEventBus reservationEventBus = event.getReservation().getReservationEventBus();
		// TODO implement
	}

	@Subscribe
	public void onReservationEnded(final ReservationEndedEvent event) {
		log.trace("PortalEventStoreServiceImpl.onReservationEnded()"); // TODO remove when working
		// TODO implement
	}
}
