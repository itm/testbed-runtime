package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStore;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public interface Reservation extends Service {

	public static class Entry {

		private NodeUrnPrefix nodeUrnPrefix;

		private String key;

		private String username;

		private Set<NodeUrn> nodeUrns;

		private Interval interval;

		private ReservationEventBus reservationEventBus;

		public Entry(final NodeUrnPrefix nodeUrnPrefix, final String username, final String key,
					 final Set<NodeUrn> nodeUrns, final Interval interval,
					 final ReservationEventBus reservationEventBus) {
			this.nodeUrnPrefix = nodeUrnPrefix;
			this.username = username;
			this.key = key;
			this.nodeUrns = nodeUrns;
			this.interval = interval;
			this.reservationEventBus = reservationEventBus;
		}

		public Interval getInterval() {
			return interval;
		}

		public String getKey() {
			return key;
		}

		public NodeUrnPrefix getNodeUrnPrefix() {
			return nodeUrnPrefix;
		}

		public Set<NodeUrn> getNodeUrns() {
			return nodeUrns;
		}

		public ReservationEventBus getReservationEventBus() {
			return reservationEventBus;
		}

		public String getUsername() {
			return username;
		}

	}

	Set<Reservation.Entry> getEntries();

	Set<NodeUrnPrefix> getNodeUrnPrefixes();

	Set<NodeUrn> getNodeUrns();

	ReservationEventBus getEventBus();

	Interval getInterval();

	@Nullable
	DateTime getCancelled();

	@Nullable
	DateTime getFinalized();

	boolean isFinalized();

    boolean isCancelled();

	String getSerializedKey();

    List<MessageLite> getPastLifecycleEvents();

	Set<SecretReservationKey> getSecretReservationKeys();

	Set<ConfidentialReservationData> getConfidentialReservationData();

	/**
	 * Creates a response tracker for the given {@code requestId}. ResponseTracker instances are held in a cache until
	 * a certain reasonable time limit and will then be removed.
	 *
	 * @param request
	 * 		the request to track
	 *
	 * @return a newly created ResponseTracker instance
	 *
	 * @throws IllegalArgumentException
	 * 		if an entry for the given requestId already exists
	 */
	ResponseTracker createResponseTracker(Request request);

	/**
	 * Gets a response tracker for the given {@code requestId}. ResponseTracker instances are held in a cache until a
	 * certain reasonable time limit and will then be removed.
	 *
	 * @param requestId
	 * 		the requestId of the request
	 *
	 * @return a ResponseTracker instance for the given {@code requestId} or {@code null} if no ResponseTracker instance
	 * was found
	 */
	ResponseTracker getResponseTracker(long requestId);

	void enableVirtualization();

	void disableVirtualization();

	boolean isVirtualizationEnabled();

	/**
	 * Returns the {@link ReservationEventStore} associated with this reservation.
	 *
	 * @return an event store
	 */
	ReservationEventStore getEventStore();
}
