package de.uniluebeck.itm.tr.iwsn.portal;

public class ReservationUnknownException extends Exception {

	private final String secretReservationKey;

	public ReservationUnknownException(final String secretReservationKey) {
		this.secretReservationKey = secretReservationKey;
	}

	public ReservationUnknownException(final String secretReservationKey, final Throwable cause) {
		super(cause);
		this.secretReservationKey = secretReservationKey;
	}

	@Override
	public String getMessage() {
		return "The reservation with the secret reservation key \"" + secretReservationKey + "\" was not found!";
	}
}
