package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.rs.ConfidentialReservationData;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

@XmlRootElement
public class ConfidentialReservationDataList {

	public List<ConfidentialReservationData> reservations;

	public ConfidentialReservationDataList() {
	}

	public ConfidentialReservationDataList(final List<ConfidentialReservationData> reservations) {
		this.reservations = reservations;
	}

	public ConfidentialReservationDataList(final Set<ConfidentialReservationData> confidentialReservationData) {
		this(newArrayList(confidentialReservationData));
	}
}
