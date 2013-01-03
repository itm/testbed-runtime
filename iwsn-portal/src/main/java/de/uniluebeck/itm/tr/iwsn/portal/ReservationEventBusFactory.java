package de.uniluebeck.itm.tr.iwsn.portal;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

public interface ReservationEventBusFactory {

	ReservationEventBus create(Set<NodeUrn> reservedNodeUrns);

}
