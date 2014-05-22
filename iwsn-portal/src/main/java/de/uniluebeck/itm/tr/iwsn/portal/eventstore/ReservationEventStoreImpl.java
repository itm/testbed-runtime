package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.eventstore.IEventStore;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;

class ReservationEventStoreImpl extends AbstractService implements ReservationEventStore {

	private static final Logger log = LoggerFactory.getLogger(ReservationEventStoreImpl.class);

	private IEventStore eventStore;

	private Reservation reservation;

	private final ReservationEventBus reservationEventBus;

	private final PortalEventStoreHelper portalEventStoreHelper;

	@Inject
	public ReservationEventStoreImpl(final PortalEventStoreHelper portalEventStoreHelper,
									 @Assisted final Reservation reservation) {
		this.reservation = reservation;
		this.reservationEventBus = reservation.getReservationEventBus();
		this.portalEventStoreHelper = portalEventStoreHelper;
	}

	@Override
	protected void doStart() {
		log.trace("ReservationEventStoreImpl.doStart()");
		try {
			eventStore = portalEventStoreHelper.createAndConfigureEventStore(reservation.getSerializedKey());
			reservationEventBus.register(this);
			notifyStarted();
		} catch (FileNotFoundException e) {
			log.error("Exception while starting reservation event store: ", e);
			notifyFailed(e);
		} catch (ClassNotFoundException e) {
			log.error("Exception while starting reservation event store: ", e);
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("ReservationEventStoreImpl.doStop()");
		reservationEventBus.unregister(this);
		try {
			eventStore.close();
		} catch (IOException e) {
			log.warn("Exception on closing event store.", e);
		}
		notifyStopped();
	}


	@Override
	public void reservationStarted(final ReservationStartedEvent event) {
		storeEvent(event);
	}

	@Override
	public void reservationEnded(final ReservationEndedEvent event) {
		storeEvent(event);
	}

	@Subscribe
	public void onEvent(final DevicesAttachedEvent event) {
		storeEvent(event);
	}

	@Subscribe
	public void onEvent(final DevicesDetachedEvent event) {
		storeEvent(event);
	}

	@Subscribe
	public void onEvent(final UpstreamMessageEvent event) {
		storeEvent(event);
	}

	@Subscribe
	public void onEvent(final NotificationEvent event) {
		storeEvent(event);
	}

	@Subscribe
	public void onEvent(final SingleNodeResponse response) {
		storeEvent(response);
	}

	@Subscribe
	public void onEvent(final GetChannelPipelinesResponse response) {
		storeEvent(response);
	}

	@Subscribe
	public void onRequest(final Request request) {
		storeEvent(request);
	}

	private void storeEvent(final MessageLite event) {
		log.trace("ReservationEventStoreImpl.storeEvent({})", event);
		try {
			storeEvent(event, event.getClass());
		} catch (IOException e) {
			log.error("Failed to store event", e);
		}
	}

	@Override
	public void storeEvent(@Nonnull Object object) throws IOException {
		//noinspection unchecked
		eventStore.storeEvent(object);
	}

	@Override
	public void storeEvent(@Nonnull Object object, Class type) throws IOException {
		//noinspection unchecked
		eventStore.storeEvent(object, type);
	}

	@Override
	public CloseableIterator<IEventContainer> getEventsBetweenTimestamps(long fromTime, long toTime)
			throws IOException {
		//noinspection unchecked
		return eventStore.getEventsBetweenTimestamps(fromTime, toTime);
	}

	@Override
	public CloseableIterator<IEventContainer> getEventsFromTimestamp(long fromTime) throws IOException {
		//noinspection unchecked
		return eventStore.getEventsFromTimestamp(fromTime);
	}

	@Override
	public CloseableIterator<IEventContainer> getAllEvents() throws IOException {
		//noinspection unchecked
		return eventStore.getAllEvents();
	}

    @Override
    public long actualPayloadByteSize() throws IOException {
        return eventStore.actualPayloadByteSize();
    }

    @Override
    public long size() {
        return eventStore.size();
    }

    @Override
	public void close() throws IOException {
		stop();
	}

	@Override
	public String toString() {
		return ReservationEventStoreImpl.class.getName() + "[" + reservation.getSerializedKey() + "]";
	}
}
