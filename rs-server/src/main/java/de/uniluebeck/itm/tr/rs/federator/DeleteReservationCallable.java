package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class DeleteReservationCallable implements Callable<Void> {

	private final List<SecretReservationKey> reservationsToBeDeleted;

	private RS rs;

	public DeleteReservationCallable(final RS rs, final List<SecretReservationKey> reservationsToBeDeleted) {
		this.rs = rs;
		this.reservationsToBeDeleted = reservationsToBeDeleted;
	}

	public List<SecretReservationKey> getReservationsToBeDeleted() {
		return reservationsToBeDeleted;
	}

	public RS getRs() {
		return rs;
	}

	@Override
	public Void call() throws Exception {
		rs.deleteReservation(reservationsToBeDeleted);
		return null;
	}
}