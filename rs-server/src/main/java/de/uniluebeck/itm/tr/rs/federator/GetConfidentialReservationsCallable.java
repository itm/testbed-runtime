package de.uniluebeck.itm.tr.rs.federator;

import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;

public class GetConfidentialReservationsCallable implements Callable<List<ConfidentialReservationData>> {

	private final XMLGregorianCalendar from;

	private final XMLGregorianCalendar to;

	private final List<SecretAuthenticationKey> secretAuthenticationKeys;

	private final RS rs;

	public GetConfidentialReservationsCallable(final RS rs,
											   final List<SecretAuthenticationKey> secretAuthenticationKeys,
											   final XMLGregorianCalendar from, final XMLGregorianCalendar to) {
		this.rs = rs;
		this.from = from;
		this.to = to;
		this.secretAuthenticationKeys = secretAuthenticationKeys;
	}

	@Override
	public List<ConfidentialReservationData> call() throws Exception {
		return rs.getConfidentialReservations(secretAuthenticationKeys, from, to);
	}
}