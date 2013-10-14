package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.common.SecretReservationKey;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

@XmlRootElement
public class SecretReservationKeyListRs {

	public List<SecretReservationKey> reservations;

	public SecretReservationKeyListRs() {
	}

	public SecretReservationKeyListRs(List<SecretReservationKey> reservations) {
		this.reservations = reservations;
	}

	public SecretReservationKeyListRs(Set<SecretReservationKey> reservations) {
		this(newArrayList(reservations));
	}
}
