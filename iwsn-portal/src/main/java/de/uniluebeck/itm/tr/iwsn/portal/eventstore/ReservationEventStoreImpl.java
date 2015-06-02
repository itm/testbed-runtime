package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class ReservationEventStoreImpl extends AbstractService implements ReservationEventStore {

	private static final Logger log = LoggerFactory.getLogger(ReservationEventStoreImpl.class);

	private final PortalEventStoreHelper helper;
	private final Reservation reservation;
	private EventStore<Message> eventStore;

	@Inject
	public ReservationEventStoreImpl(final PortalEventStoreHelper helper, @Assisted final Reservation reservation) {
		this.helper = helper;
		this.reservation = reservation;
	}

	@Override
	protected void doStart() {
		log.trace("ReservationEventStoreImpl.doStart()");
		try {
			// if service is restarted while a reservation is running (or after the store has been created but the
			// reservation didn't start yet we might have to load an existing instead of creating a new store...
			openEventStore();
			reservation.getEventBus().register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private void openEventStore() throws IOException, ClassNotFoundException {
		if (helper.eventStoreExistsForReservation(reservation.getSerializedKey())) {
			eventStore = helper.loadEventStore(reservation.getSerializedKey(), false);
		} else {
			eventStore = helper.createAndConfigureEventStore(reservation.getSerializedKey());
		}
	}

	@Override
	protected void doStop() {
		log.trace("ReservationEventStoreImpl.doStop()");
		reservation.getEventBus().unregister(this);
		closeEventStore();
		notifyStopped();
	}

	private void closeEventStore() {
		try {
			eventStore.close();
		} catch (IOException e) {
			log.warn("Exception on closing event store.", e);
		}
	}

	@Subscribe
	public void onObject(final Object obj) {
		if (MessageHeaderPair.isUnwrappedMessageEvent(obj)) {
			MessageHeaderPair pair = MessageHeaderPair.fromUnwrapped(obj);
			try {
				log.trace("Storing {} event to reservation event store.", pair.header.getType());
				eventStore.storeEvent(MessageWrapper.wrap(pair.message), Message.class, pair.header.getTimestamp());
			} catch (IOException e) {
				log.error("Failed to store event", e);
			}
		}
	}

	@Override
	public String toString() {
		return ReservationEventStoreImpl.class.getName() + "[" + reservation.getSerializedKey() + "]";
	}

	@Override
	public CloseableIterator<MessageHeaderPair> getEvents() throws IOException {
		if (!isRunning()) {
			try {
				openEventStore();
				return new CloseableMessageHeaderPairIterator(eventStore.getAllEvents());
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				closeEventStore();
			}
		}
		return new CloseableMessageHeaderPairIterator(eventStore.getAllEvents());
	}

	@Override
	public CloseableIterator<MessageHeaderPair> getEventsBetween(long startTime, long endTime) throws IOException {
		if (!isRunning()) {
			try {
				openEventStore();
				return new CloseableMessageHeaderPairIterator(eventStore.getEventsBetweenTimestamps(startTime, endTime));
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				closeEventStore();
			}
		}
		return new CloseableMessageHeaderPairIterator(eventStore.getEventsBetweenTimestamps(startTime, endTime));
	}


}
