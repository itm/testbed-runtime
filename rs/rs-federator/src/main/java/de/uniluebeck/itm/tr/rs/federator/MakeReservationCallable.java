package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.SecretAuthenticationKey;
import eu.wisebed.api.rs.SecretReservationKey;

import java.util.List;
import java.util.concurrent.Callable;

public class MakeReservationCallable implements Callable<List<SecretReservationKey>> {

	private final RS rs;

	private final List<SecretAuthenticationKey> secretAuthenticationKeys;

	private final ConfidentialReservationData reservation;

	public MakeReservationCallable(final RS rs, final List<SecretAuthenticationKey> secretAuthenticationKeys,
									final ConfidentialReservationData reservation) {
		this.rs = rs;
		this.secretAuthenticationKeys = secretAuthenticationKeys;
		this.reservation = reservation;
	}

	public ConfidentialReservationData getReservation() {
		return reservation;
	}

	public RS getRs() {
		return rs;
	}

	public List<SecretAuthenticationKey> getSecretAuthenticationKeys() {
		return secretAuthenticationKeys;
	}

	@Override
	public List<SecretReservationKey> call() throws Exception {
		return rs.makeReservation(secretAuthenticationKeys, reservation);
	}
}