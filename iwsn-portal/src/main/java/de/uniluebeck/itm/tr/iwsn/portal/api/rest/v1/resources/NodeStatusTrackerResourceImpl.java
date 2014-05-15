package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.NodeStatusDto;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.FlashStatus;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.NodeStatusTracker;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.ReservationStatus;
import de.uniluebeck.itm.util.Tuple;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class NodeStatusTrackerResourceImpl implements NodeStatusTrackerResource {

	private final NodeStatusTracker nodeStatusTracker;

	@Inject
	public NodeStatusTrackerResourceImpl(final NodeStatusTracker nodeStatusTracker) {
		this.nodeStatusTracker = nodeStatusTracker;
	}

	@Override
	public List<NodeStatusDto> getNodeStatusList() {
		final ImmutableSet<Map.Entry<NodeUrn,Tuple<FlashStatus,ReservationStatus>>> entries =
				nodeStatusTracker.getStatusMap().entrySet();
		final List<NodeStatusDto> list = newArrayList();
		for (Map.Entry<NodeUrn, Tuple<FlashStatus, ReservationStatus>> entry : entries) {
			list.add(new NodeStatusDto(
					entry.getKey().toString(),
					entry.getValue().getFirst(),
					entry.getValue().getSecond()
			)
			);
		}
		return list;
	}
}
