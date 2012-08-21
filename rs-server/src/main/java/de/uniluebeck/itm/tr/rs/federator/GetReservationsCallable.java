package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.v3.rs.PublicReservationData;
import eu.wisebed.api.v3.rs.RS;
import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.Callable;

public class GetReservationsCallable implements Callable<List<PublicReservationData>> {

	private final RS rs;

	private final DateTime from;

	private final DateTime to;

	public GetReservationsCallable(final RS rs, final DateTime from, final DateTime to) {
		this.rs = rs;
		this.from = from;
		this.to = to;
	}

	@Override
	public List<PublicReservationData> call() throws Exception {
		return rs.getReservations(from, to);
	}
}