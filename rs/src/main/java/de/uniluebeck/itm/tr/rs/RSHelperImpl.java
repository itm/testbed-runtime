package de.uniluebeck.itm.tr.rs;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.NodeUrnHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.rs.PublicReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Interval;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static eu.wisebed.wiseml.WiseMLHelper.getNodeUrns;

public class RSHelperImpl implements RSHelper {

	private final RS rs;

	private final SessionManagement sm;

	@Inject
	public RSHelperImpl(final RS rs, final SessionManagement sm) {
		this.rs = rs;
		this.sm = sm;
	}

	@Override
	public Set<NodeUrn> getNodes() {
		return newHashSet(transform(getNodeUrns(sm.getNetwork()), NodeUrnHelper.STRING_TO_NODE_URN));
	}

	@Override
	public Set<NodeUrn> getUnreservedNodes() {
		return difference(getNodes(), getReservedNodes());
	}

	@Override
	public Set<NodeUrn> getUnreservedNodes(final Duration duration) {
		return difference(getNodes(), getReservedNodes(duration));
	}

	@Override
	public Set<NodeUrn> getUnreservedNodes(final Instant instant) {
		return difference(getNodes(), getReservedNodes(instant));
	}

	@Override
	public Set<NodeUrn> getUnreservedNodes(final Interval interval) {
		return difference(getNodes(), getReservedNodes(interval));
	}

	@Override
	public Set<NodeUrn> getReservedNodes() {
		final DateTime now = DateTime.now();
		return getReservedNodes(new Interval(now, now));
	}

	@Override
	public Set<NodeUrn> getReservedNodes(final Duration duration) {
		final DateTime now = DateTime.now();
		return getReservedNodes(new Interval(now, now.plus(duration)));
	}

	@Override
	public Set<NodeUrn> getReservedNodes(final Instant instant) {
		return getReservedNodes(new Interval(instant.toDateTime(), instant.toDateTime()));
	}

	@Override
	public Set<NodeUrn> getReservedNodes(final Interval interval) {
		try {
			return toNodeUrnSet(rs.getReservations(interval.getStart(), interval.getEnd(), null, null, false));
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	private Set<NodeUrn> toNodeUrnSet(final List<PublicReservationData> reservations) {
		final Set<NodeUrn> nodeUrnSet = newHashSet();
		for (PublicReservationData reservation : reservations) {
			nodeUrnSet.addAll(reservation.getNodeUrns());
		}
		return nodeUrnSet;
	}
}
