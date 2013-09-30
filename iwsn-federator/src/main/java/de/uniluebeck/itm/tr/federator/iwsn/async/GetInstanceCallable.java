package de.uniluebeck.itm.tr.federator.iwsn.async;

import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.SessionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

public class GetInstanceCallable implements Callable<GetInstanceCallable.Result> {

	private static final Logger log = LoggerFactory.getLogger(GetInstanceCallable.class);

	public static class Result {

		public URI federatedWSNInstanceEndpointUrl;

		public List<SecretReservationKey> secretReservationKey;

		private Result(final List<SecretReservationKey> secretReservationKey,
					   final URI federatedWSNInstanceEndpointUrl) {
			this.secretReservationKey = secretReservationKey;
			this.federatedWSNInstanceEndpointUrl = federatedWSNInstanceEndpointUrl;
		}

	}

	private final URI uri;

	private SessionManagement sm;

	private List<SecretReservationKey> secretReservationKeys;

	public GetInstanceCallable(final URI uri, final SessionManagement sm,
							   final List<SecretReservationKey> secretReservationKeys) {
		this.uri = uri;
		this.sm = sm;
		this.secretReservationKeys = secretReservationKeys;
	}

	@Override
	public GetInstanceCallable.Result call() throws Exception {
		log.debug("Calling getInstance on {}", uri);
		return new GetInstanceCallable.Result(secretReservationKeys, URI.create(sm.getInstance(secretReservationKeys)));
	}
}