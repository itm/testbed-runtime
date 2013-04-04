package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Interval;

import java.util.Set;

public interface Reservation extends Service {

	String getKey();

	String getUsername();

	Set<NodeUrn> getNodeUrns();

	ReservationEventBus getEventBus();

	Interval getInterval();

	void enableVirtualization();

	void disableVirtualization();
}
