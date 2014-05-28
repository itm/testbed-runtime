package de.uniluebeck.itm.tr.common.events;

import eu.wisebed.api.v3.common.SecretReservationKey;

import java.util.Set;

public abstract class ReservationEvent {

	private String serializedKey;

	private Set<SecretReservationKey> secretReservationKeys;

	public ReservationEvent(final String serializedKey,
							final Set<SecretReservationKey> secretReservationKeys) {
		this.serializedKey = serializedKey;
		this.secretReservationKeys = secretReservationKeys;
	}

	public String getSerializedKey() {
		return serializedKey;
	}

	public Set<SecretReservationKey> getSecretReservationKeys() {
		return secretReservationKeys;
	}
}
