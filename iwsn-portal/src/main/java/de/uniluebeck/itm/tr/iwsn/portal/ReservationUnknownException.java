package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.base.Joiner;
import eu.wisebed.api.v3.common.SecretReservationKey;

import java.util.Set;

public class ReservationUnknownException extends Exception {

	private final Set<SecretReservationKey> secretReservationKeys;

	public ReservationUnknownException(final Set<SecretReservationKey> secretReservationKeys) {
		this.secretReservationKeys = secretReservationKeys;
	}

	public ReservationUnknownException(final Set<SecretReservationKey> secretReservationKeys, final Throwable cause) {
		super(cause);
		this.secretReservationKeys = secretReservationKeys;
	}

	@Override
	public String getMessage() {
		return "The reservation with the secret reservation keys [" +
				Joiner.on(",").join(secretReservationKeys) +
				"] was not found!";
	}

	public Set<SecretReservationKey> getSecretReservationKeys() {
		return secretReservationKeys;
	}
}
