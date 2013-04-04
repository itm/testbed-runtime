package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class ReservationImpl extends AbstractService implements Reservation {

	private static final Logger log = LoggerFactory.getLogger(Reservation.class);

	private final PortalEventBus portalEventBus;

	private final Set<NodeUrn> nodeUrns;

	private final ReservationEventBus reservationEventBus;

	private final Interval interval;

	private final String username;

	private final String key;

	@Inject
	public ReservationImpl(final ReservationEventBusFactory reservationEventBusFactory,
						   final PortalEventBus portalEventBus,
						   @Assisted("secretReservationKey") final String key,
						   @Assisted("username") final String username,
						   @Assisted final Set<NodeUrn> nodeUrns,
						   @Assisted final Interval interval) {
		this.key = checkNotNull(key);
		this.username = checkNotNull(username);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.nodeUrns = checkNotNull(nodeUrns);
		this.interval = checkNotNull(interval);
		this.reservationEventBus = checkNotNull(reservationEventBusFactory.create(this));
	}

	@Override
	protected void doStart() {
		log.trace("ReservationImpl.doStart()");
		try {
			reservationEventBus.startAndWait();
			notifyStarted();
			portalEventBus.post(new ReservationStartedEvent(this));
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("ReservationImpl.doStop()");
		try {
			portalEventBus.post(new ReservationEndedEvent(this));
			reservationEventBus.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public String getKey() {
		return key;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public Set<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}

	@Override
	public ReservationEventBus getEventBus() {
		return reservationEventBus;
	}

	@Override
	public Interval getInterval() {
		return interval;
	}

	@Override
	public void enableVirtualization() {
		reservationEventBus.enableVirtualization();
	}

	@Override
	public void disableVirtualization() {
		reservationEventBus.disableVirtualization();
	}

	@Override
	public boolean equals(final Object o) {

		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final ReservationImpl that = (ReservationImpl) o;
		return interval.equals(that.interval) && nodeUrns.equals(that.nodeUrns);
	}

	@Override
	public int hashCode() {
		int result = nodeUrns.hashCode();
		result = 31 * result + interval.hashCode();
		return result;
	}
}
