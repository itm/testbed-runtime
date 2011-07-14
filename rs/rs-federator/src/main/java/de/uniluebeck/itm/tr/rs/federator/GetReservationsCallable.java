package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.rs.PublicReservationData;
import eu.wisebed.api.rs.RS;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;

public class GetReservationsCallable implements Callable<List<PublicReservationData>> {

	private final RS rs;

	private final XMLGregorianCalendar from;

	private final XMLGregorianCalendar to;

	public GetReservationsCallable(final RS rs, final XMLGregorianCalendar from, final XMLGregorianCalendar to) {
		this.rs = rs;
		this.from = from;
		this.to = to;
	}

	@Override
	public List<PublicReservationData> call() throws Exception {
		return rs.getReservations(from, to);
	}
}