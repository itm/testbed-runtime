package de.uniluebeck.itm.tr.federator.rs;

import eu.wisebed.api.v3.rs.PublicReservationData;
import eu.wisebed.api.v3.rs.RS;
import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.Callable;

public class GetReservationsCallable implements Callable<List<PublicReservationData>> {

	private final RS rs;

	private final DateTime from;

	private final DateTime to;

	private final Integer offset;

	private final Integer amount;

	private final Boolean showCancelled;

	public GetReservationsCallable(final RS rs, final DateTime from, final DateTime to,
								   final Integer offset, final Integer amount, final Boolean showCancelled) {
		this.rs = rs;
		this.from = from;
		this.to = to;
		this.offset = offset;
		this.amount = amount;
		this.showCancelled = showCancelled;
	}

	@Override
	public List<PublicReservationData> call() throws Exception {
		return rs.getReservations(from, to, offset, amount, showCancelled);
	}
}