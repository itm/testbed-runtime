package de.uniluebeck.itm.tr.federator.rs;

import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.RS;
import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.Callable;

import static com.google.common.collect.Lists.newArrayList;

public class MakeReservationCallable implements Callable<List<SecretReservationKey>> {

	private final RS rs;

	private final List<SecretAuthenticationKey> secretAuthenticationKeys;

	private final List<NodeUrn> nodeUrns;

	private final DateTime from;

	private final DateTime to;

	private final String description;

	private final List<KeyValuePair> options;

	public MakeReservationCallable(final RS rs, final DateTime from, final DateTime to, final String description,
								   final List<KeyValuePair> options) {
		this.rs = rs;
		this.from = from;
		this.to = to;
		this.description = description;
		this.options = options;

		this.secretAuthenticationKeys = newArrayList();
		this.nodeUrns = newArrayList();
	}

	public RS getRs() {
		return rs;
	}

	public String getDescription() {
		return description;
	}

	public DateTime getFrom() {
		return from;
	}

	public List<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}

	public List<KeyValuePair> getOptions() {
		return options;
	}

	public List<SecretAuthenticationKey> getSecretAuthenticationKeys() {
		return secretAuthenticationKeys;
	}

	public DateTime getTo() {
		return to;
	}

	@Override
	public List<SecretReservationKey> call() throws Exception {
		return rs.makeReservation(secretAuthenticationKeys, nodeUrns, from, to, description, options);
	}
}