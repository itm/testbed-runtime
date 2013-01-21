package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.Callable;

public class GetConfidentialReservationsCallable implements Callable<List<ConfidentialReservationData>> {

	private final DateTime from;

	private final DateTime to;

	private final List<SecretAuthenticationKey> secretAuthenticationKeys;

	private final RS rs;

	public GetConfidentialReservationsCallable(final RS rs,
											   final List<SecretAuthenticationKey> secretAuthenticationKeys,
											   final DateTime from, final DateTime to) {
		this.rs = rs;
		this.from = from;
		this.to = to;
		this.secretAuthenticationKeys = secretAuthenticationKeys;
	}

	@Override
	public List<ConfidentialReservationData> call() throws Exception {
		return rs.getConfidentialReservations(secretAuthenticationKeys, from, to);
	}
}