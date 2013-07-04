package de.uniluebeck.itm.tr.federator.iwsn;

import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.SessionManagement;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

class GetInstanceCallable implements Callable<GetInstanceCallable.Result> {

	public static class Result {

		public URI federatedWSNInstanceEndpointUrl;

		public List<SecretReservationKey> secretReservationKey;

		public URI controller;

		private Result(final List<SecretReservationKey> secretReservationKey,
					   final URI controller,
					   final URI federatedWSNInstanceEndpointUrl) {
			this.secretReservationKey = secretReservationKey;
			this.controller = controller;
			this.federatedWSNInstanceEndpointUrl = federatedWSNInstanceEndpointUrl;
		}

	}

	private SessionManagement sm;

	private List<SecretReservationKey> secretReservationKey;

	private URI controller;

	public GetInstanceCallable(final SessionManagement sm,
							   final List<SecretReservationKey> secretReservationKey,
							   final URI controller) {
		this.sm = sm;
		this.secretReservationKey = secretReservationKey;
		this.controller = controller;
	}

	@Override
	public GetInstanceCallable.Result call() throws Exception {
		URI federatedWSNInstanceEndpointUrl = URI.create(sm.getInstance(secretReservationKey));
		return new GetInstanceCallable.Result(secretReservationKey, controller, federatedWSNInstanceEndpointUrl);
	}
}