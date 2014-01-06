package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions;

import org.joda.time.Interval;
import org.joda.time.format.ISODateTimeFormat;

public class ReservationNotRunningException extends Exception {

	private final Interval interval;

	public ReservationNotRunningException(final Interval interval) {
		super("Reservation not running. Reservation interval:  " +
				interval.getStart().toString(ISODateTimeFormat.dateTime()) +
				" until " +
				interval.getEnd().toString(ISODateTimeFormat.dateTime())
		);
		this.interval = interval;
	}

	public Interval getInterval() {
		return interval;
	}
}
