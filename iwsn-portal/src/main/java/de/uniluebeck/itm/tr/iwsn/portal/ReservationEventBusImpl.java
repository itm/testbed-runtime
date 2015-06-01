package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.messages.Header;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public class ReservationEventBusImpl extends AbstractService implements ReservationEventBus {

	private static final Logger log = LoggerFactory.getLogger(ReservationEventBus.class);

	protected final EventBus eventBus;

	protected final Reservation reservation;

	protected final String serializedReservationKey;

	@Inject
	public ReservationEventBusImpl(final EventBusFactory eventBusFactory, @Assisted final Reservation reservation) {
		this.eventBus = eventBusFactory.create("ReservationEventBus[" + reservation.getSerializedKey() + "]");
		this.reservation = reservation;
		this.serializedReservationKey = reservation.getSerializedKey();
	}

	@Override
	protected void doStart() {
		log.trace("ReservationEventBus[{}].doStart()", serializedReservationKey);
		try {
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("ReservationEventBus[{}].doStop()", serializedReservationKey);
		try {
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	public void register(final Object object) {
		log.trace("ReservationEventBus[{}].register(object={})", serializedReservationKey, object);
		eventBus.register(object);
	}

	@Override
	public void unregister(final Object object) {
		log.trace("ReservationEventBus[{}].unregister(object={})", serializedReservationKey, object);
		eventBus.unregister(object);
	}

	@Override
	public void post(final Object event) {

		log.trace("ReservationEventBus[{}].post({})", serializedReservationKey, event);

		checkState(isRunning(), "ReservationEventBus is not running");

		if (MessageHeaderPair.isWrappedMessageEvent(event)) {
			throw new IllegalArgumentException("Posting wrapped messages is not allowed. Please use a concrete event / request / response type!");
		}

		if (MessageHeaderPair.isUnwrappedMessageEvent(event)) {
			Header header = MessageHeaderPair.fromUnwrapped(event).header;
			assertNodesArePartOfReservation(Lists.transform(header.getNodeUrnsList(), NodeUrn::new));
		}

		eventBus.post(event);
	}

	@Override
	public void enableVirtualization() {
		throw new RuntimeException("Virtualization features are not yet implemented!");
	}

	@Override
	public void disableVirtualization() {
		throw new RuntimeException("Virtualization features are not yet implemented!");
	}

	@Override
	public boolean isVirtualizationEnabled() {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getName() + "[" + serializedReservationKey + "]";
	}

	private void assertNodesArePartOfReservation(final List<NodeUrn> nodeUrns) {
		if (!reservation.getNodeUrns().containsAll(nodeUrns)) {
			final Set<NodeUrn> unreservedNodeUrns = nodeUrns.stream()
					.filter(n -> !(reservation.getNodeUrns().contains(n)))
					.collect(Collectors.toSet());
			throw new IllegalArgumentException("The node URNs [" + Joiner.on(",").join(unreservedNodeUrns) + "] "
					+ "are not part of the reservation."
			);
		}

	}
}
