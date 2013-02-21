package de.uniluebeck.itm.tr.iwsn.portal;

import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Interval;

import java.util.Set;

public interface ReservationFactory {

	Reservation create(String username, Set<NodeUrn> nodeUrns, Interval interval);

}
