package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Interval;

import java.util.Set;

public interface Reservation extends Service {

	String getKey();

	String getUsername();

	Set<NodeUrn> getNodeUrns();

	ReservationEventBus getEventBus();

	Interval getInterval();

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
	 *         was found
	 */
	ResponseTracker getResponseTracker(long requestId);

	void enableVirtualization();

	void disableVirtualization();
}
