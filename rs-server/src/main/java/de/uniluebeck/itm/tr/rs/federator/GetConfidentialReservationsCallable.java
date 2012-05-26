package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.GetReservations;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.SecretAuthenticationKey;

import java.util.List;
import java.util.concurrent.Callable;

public class GetConfidentialReservationsCallable implements Callable<List<ConfidentialReservationData>> {

	private final GetReservations period;

	private final List<SecretAuthenticationKey> secretAuthenticationData;

	private final RS rs;

	public GetConfidentialReservationsCallable(final RS rs,
											   final List<SecretAuthenticationKey> secretAuthenticationData,
											   final GetReservations period) {
		this.rs = rs;
		this.period = period;
		this.secretAuthenticationData = secretAuthenticationData;
	}

	@Override
	public List<ConfidentialReservationData> call() throws Exception {
		return rs.getConfidentialReservations(secretAuthenticationData, period);
	}
}