package de.uniluebeck.itm.tr.federator;

import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Interval;

import java.util.Set;

public class FederatorReservation extends AbstractService implements Reservation {

	@Override
	protected void doStart() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	protected void doStop() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public Set<Entry> getEntries() {
		return null;  // TODO implement
	}

	@Override
	public Set<NodeUrn> getNodeUrns() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public ReservationEventBus getEventBus() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public Interval getInterval() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public ResponseTracker createResponseTracker(final Request request) {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public ResponseTracker getResponseTracker(final long requestId) {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public void enableVirtualization() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public void disableVirtualization() {
		throw new RuntimeException("Implement me!");
	}

	@Override
	public boolean isVirtualizationEnabled() {
		throw new RuntimeException("Implement me!");
	}
}
