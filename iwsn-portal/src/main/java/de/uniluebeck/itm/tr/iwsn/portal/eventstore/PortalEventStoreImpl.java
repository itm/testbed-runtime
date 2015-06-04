package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.adminui.EventStoreAdminService;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

class PortalEventStoreImpl extends AbstractService implements PortalEventStore {

	private static final Logger log = LoggerFactory.getLogger(PortalEventStoreImpl.class);

	private final PortalEventBus portalEventBus;
	private final ReservationManager reservationManager;
	private final PortalEventStoreHelper portalEventStoreHelper;
	private final PortalServerConfig portalServerConfig;
	private final EventStoreAdminService eventStoreAdminService;

	private EventStore<Message> eventStore;

	@Inject
	public PortalEventStoreImpl(final PortalEventBus portalEventBus,
								final ReservationManager reservationManager,
								final PortalEventStoreHelper portalEventStoreHelper,
								final PortalServerConfig portalServerConfig,
								final EventStoreAdminService eventStoreAdminService) {
		this.portalEventBus = portalEventBus;
		this.reservationManager = reservationManager;
		this.portalEventStoreHelper = portalEventStoreHelper;
		this.portalServerConfig = portalServerConfig;
		this.eventStoreAdminService = eventStoreAdminService;
	}

	@Override
	protected void doStart() {
		log.trace("PortalEventStoreServiceImpl.doStart()");
		try {
			if (portalEventStoreHelper.eventStoreExistsForReservation(portalServerConfig.getPortalEventstoreName())) {
				eventStore = portalEventStoreHelper.loadEventStore(portalServerConfig.getPortalEventstoreName(), false);
			} else {
				eventStore = portalEventStoreHelper.createAndConfigureEventStore(portalServerConfig.getPortalEventstoreName());
			}
			eventStoreAdminService.startAsync().awaitRunning();
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("PortalEventStoreServiceImpl.doStop()");
		portalEventBus.unregister(this);
		try {
			eventStoreAdminService.stopAsync().awaitTerminated();
		} catch (Exception e) {
			notifyFailed(e);
		}
		try {
			eventStore.close();
		} catch (IOException e) {
			log.warn("Exception on closing event store.", e);
		}
		try {
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onEvent(final Object obj) {
		if (MessageHeaderPair.isUnwrappedMessageEvent(obj)) {
			MessageHeaderPair pair = MessageHeaderPair.fromUnwrapped(obj);

			if (portalServerConfig.isPortalEventStorePersistReserved() || !partOfReservation(pair)) {
				try {
					log.trace("Storing {} event to portal event store.", pair.header.getType());
					eventStore.storeEvent(MessageWrapper.wrap(pair.message), Message.class, pair.header.getTimestamp());
				} catch (IOException e) {
					log.error("IOException while trying to store an event into PortalEventStore: ", e);
				}
			}
		}
	}

	private boolean partOfReservation(MessageHeaderPair pair) {
		return !reservationManager.getReservationMapping(
				newHashSet(transform(pair.header.getNodeUrnsList(), NodeUrn::new)),
				new DateTime(pair.header.getTimestamp())
		).isEmpty();
	}

	@Override
	public CloseableIterator<MessageHeaderPair> getEventsBetweenTimestamps(long from, long to) throws IOException {
		return new CloseableMessageHeaderPairIterator(eventStore.getEventsBetweenTimestamps(from, to));
	}

	@Override
	public CloseableIterator<MessageHeaderPair> getEventsFromTimestamp(long from) throws IOException {
		return new CloseableMessageHeaderPairIterator(eventStore.getEventsFromTimestamp(from));
	}

	@Override
	public CloseableIterator<MessageHeaderPair> getAllEvents() throws IOException {
		return new CloseableMessageHeaderPairIterator(eventStore.getAllEvents());
	}
}



