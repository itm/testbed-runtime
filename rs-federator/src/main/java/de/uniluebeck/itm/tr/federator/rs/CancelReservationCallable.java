package de.uniluebeck.itm.tr.federator.rs;

import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.RS;

import java.util.List;
import java.util.concurrent.Callable;

import static com.google.common.collect.Lists.newArrayList;

public class CancelReservationCallable implements Callable<Void> {

	private final List<SecretAuthenticationKey> secretAuthenticationKeys;

	private final List<SecretReservationKey> secretReservationKeys;

	private RS rs;

	public CancelReservationCallable(final RS rs) {
		this.rs = rs;
		this.secretAuthenticationKeys = newArrayList();
		this.secretReservationKeys = newArrayList();
	}

	public List<SecretReservationKey> getSecretReservationKeys() {
		return secretReservationKeys;
	}

	public RS getRs() {
		return rs;
	}

	public List<SecretAuthenticationKey> getSecretAuthenticationKeys() {
		return secretAuthenticationKeys;
	}

	@Override
	public Void call() throws Exception {
		rs.cancelReservation(secretAuthenticationKeys, secretReservationKeys);
		return null;
	}

	public void addSecretAuthenticationKey(final SecretAuthenticationKey secretAuthenticationKey) {
		this.secretAuthenticationKeys.add(secretAuthenticationKey);
	}

	public void addSecretReservationKey(final SecretReservationKey secretReservationKey) {
		this.secretReservationKeys.add(secretReservationKey);
	}
}