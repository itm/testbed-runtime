package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.RS;
import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.Callable;

public class MakeReservationCallable implements Callable<List<SecretReservationKey>> {

	private final RS rs;

	private final List<SecretAuthenticationKey> secretAuthenticationKeys;

	private final List<String> nodeUrns;

	private final DateTime from;

	private final DateTime to;

	public MakeReservationCallable(final RS rs,
								   final List<SecretAuthenticationKey> secretAuthenticationKeys,
								   final List<String> nodeUrns,
								   final DateTime from,
								   final DateTime to) {
		this.rs = rs;
		this.secretAuthenticationKeys = secretAuthenticationKeys;
		this.nodeUrns = nodeUrns;
		this.from = from;
		this.to = to;
	}

	public RS getRs() {
		return rs;
	}

	public List<String> getNodeUrns() {
		return nodeUrns;
	}

	@Override
	public List<SecretReservationKey> call() throws Exception {
		return rs.makeReservation(secretAuthenticationKeys, nodeUrns, from, to);
	}
}