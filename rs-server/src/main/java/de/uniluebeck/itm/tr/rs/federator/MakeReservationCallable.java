package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;

public class MakeReservationCallable implements Callable<List<SecretReservationKey>> {

	private final RS rs;

	private final List<SecretAuthenticationKey> secretAuthenticationKeys;

	private final List<String> nodeUrns;

	private final XMLGregorianCalendar from;

	private final XMLGregorianCalendar to;

	public MakeReservationCallable(final RS rs,
								   final List<SecretAuthenticationKey> secretAuthenticationKeys,
								   final List<String> nodeUrns,
								   final XMLGregorianCalendar from,
								   final XMLGregorianCalendar to) {
		this.rs = rs;
		this.secretAuthenticationKeys = secretAuthenticationKeys;
		this.nodeUrns = nodeUrns;
		this.from = from;
		this.to = to;
	}

	public RS getRs() {
		return rs;
	}

	public List<SecretAuthenticationKey> getSecretAuthenticationKeys() {
		return secretAuthenticationKeys;
	}

	public XMLGregorianCalendar getFrom() {
		return from;
	}

	public List<String> getNodeUrns() {
		return nodeUrns;
	}

	public XMLGregorianCalendar getTo() {
		return to;
	}

	@Override
	public List<SecretReservationKey> call() throws Exception {
		return rs.makeReservation(secretAuthenticationKeys, nodeUrns, from, to);
	}
}