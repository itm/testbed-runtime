package de.uniluebeck.itm.tr.rs;

import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Interval;

import java.util.Set;

public interface RSHelper {

	Set<NodeUrn> getNodes();

	Set<NodeUrn> getUnreservedNodes();

	Set<NodeUrn> getUnreservedNodes(Duration duration);

	Set<NodeUrn> getUnreservedNodes(Instant instant);

	Set<NodeUrn> getUnreservedNodes(Interval interval);

	Set<NodeUrn> getReservedNodes();

	Set<NodeUrn> getReservedNodes(Duration duration);

	Set<NodeUrn> getReservedNodes(Instant instant);

	Set<NodeUrn> getReservedNodes(Interval interval);
}
