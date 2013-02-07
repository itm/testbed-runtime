package de.uniluebeck.itm.tr.iwsn.portal;

public class ReservationUnknownException extends Exception {

	private final long secretReservationKey;

	public ReservationUnknownException(final long secretReservationKey) {
		this.secretReservationKey = secretReservationKey;
	}

	public ReservationUnknownException(final long secretReservationKey, final Throwable cause) {
		super(cause);
		this.secretReservationKey = secretReservationKey;
	}

	@Override
	public String getMessage() {
		return "The reservation with the secret reservation key \"" + secretReservationKey + "\" was not found!";
	}
}
