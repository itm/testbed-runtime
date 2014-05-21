package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

class PortalEventStoreServiceImpl extends AbstractService implements PortalEventStoreService {

	private static final Logger log = LoggerFactory.getLogger(PortalEventStoreServiceImpl.class);

	private final HashMap<String, ReservationEventStore> reservationStores =
			new HashMap<String, ReservationEventStore>();

	private final PortalEventBus portalEventBus;

	private final ReservationEventStoreFactory reservationEventStoreFactory;

	private final PortalEventStoreHelper portalEventStoreHelper;

	private final ReservationManager reservationManager;

	private final Object reservationStoresLock = new Object();

	@Inject
	public PortalEventStoreServiceImpl(final PortalEventBus portalEventBus,
									   final ReservationEventStoreFactory reservationEventStoreFactory,
									   final PortalEventStoreHelper portalEventStoreHelper,
									   final ReservationManager reservationManager) {
		this.portalEventBus = portalEventBus;
		this.reservationEventStoreFactory = reservationEventStoreFactory;
		this.portalEventStoreHelper = portalEventStoreHelper;
		this.reservationManager = reservationManager;
	}

	@Override
	protected void doStart() {
		log.trace("PortalEventStoreServiceImpl.doStart()");
		try {
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
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onReservationStarted(final ReservationStartedEvent event) {
		synchronized (reservationStoresLock) {
			Reservation reservation = reservationManager.getReservation(event.getSerializedKey());
			ReservationEventStore reservationEventStore = reservationEventStoreFactory.create(reservation);
			reservationEventStore.startAndWait();
			reservationStores.put(event.getSerializedKey(), reservationEventStore);
			reservationEventStore.reservationStarted(event);
		}
	}

	@Subscribe
	public void onReservationEnded(final ReservationEndedEvent event) {
		synchronized (reservationStoresLock) {
			ReservationEventStore reservationEventStore;
			reservationEventStore = reservationStores.remove(event.getSerializedKey());
			if (reservationEventStore != null) {
				reservationEventStore.reservationEnded(event);
				reservationEventStore.stop();
			}
		}
	}

	private IEventStore getEventStore(String serializedReservationKey) throws IOException {
		final Reservation reservation = reservationManager.getReservation(serializedReservationKey);
		if (reservation.isRunning()) {
			synchronized (reservationStoresLock) {
				return reservationStores.get(reservation.getSerializedKey());
			}
		} else {
			return portalEventStoreHelper.loadEventStore(serializedReservationKey);
		}
	}

	@Override
	public CloseableIterator<IEventContainer> getEventsBetween(String serializedReservationKey, long startTime,
															   long endTime) throws IOException {
		IEventStore store = getEventStore(serializedReservationKey);
		//noinspection unchecked
		return store.getEventsBetweenTimestamps(startTime, endTime);
	}

	@Override
	public CloseableIterator<IEventContainer> getEvents(String serializedReservationKey) throws IOException {
		IEventStore store = getEventStore(serializedReservationKey);
		//noinspection unchecked
		return store.getAllEvents();
	}
}
