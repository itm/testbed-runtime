package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.util.Tuple;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

/**
 * Helper class that tracks the status of testbed nodes regarding if they are reserved and with what type of image.
 */
public interface NodeStatusTracker extends Runnable, Service {

	void setFlashStatus(NodeUrn nodeUrn, FlashStatus flashStatus);

	FlashStatus getFlashStatus(NodeUrn nodeUrn);

	ImmutableMap<NodeUrn, Tuple<FlashStatus, ReservationStatus>> getStatusMap();

	ReservationStatus getReservationStatus(NodeUrn nodeUrn);

	Set<NodeUrn> getNodes(FlashStatus flashStatus);

	Set<NodeUrn> getNodes(ReservationStatus reservationStatus);

	Set<NodeUrn> getNodes(FlashStatus flashStatus, ReservationStatus reservationStatus);

}
