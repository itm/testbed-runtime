package de.uniluebeck.itm.wisebed.cmdlineclient;

public class ReservationKey {

	private final String secretReservationKey;

	private final String urnPrefix;

	public ReservationKey(final String secretReservationKey, final String urnPrefix) {
		this.secretReservationKey = secretReservationKey;
		this.urnPrefix = urnPrefix;
	}

	public String getSecretReservationKey() {
		return secretReservationKey;
	}

	public String getUrnPrefix() {
		return urnPrefix;
	}
}
