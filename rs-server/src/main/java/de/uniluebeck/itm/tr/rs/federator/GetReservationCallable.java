package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.SecretReservationKey;

import java.util.List;
import java.util.concurrent.Callable;

public class GetReservationCallable implements Callable<List<ConfidentialReservationData>> {

	private final RS rs;

	private final List<SecretReservationKey> secretReservationKeys;

	public GetReservationCallable(final RS rs, final List<SecretReservationKey> secretReservationKeys) {
		this.rs = rs;
		this.secretReservationKeys = secretReservationKeys;
	}

	@Override
	public List<ConfidentialReservationData> call() throws Exception {
		return rs.getReservation(secretReservationKeys);
	}
}