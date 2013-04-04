package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.rs.ConfidentialReservationData;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ConfidentialReservationDataList {

	public List<ConfidentialReservationData> reservations;

	public ConfidentialReservationDataList() {
	}

	public ConfidentialReservationDataList(List<ConfidentialReservationData> reservations) {
		this.reservations = reservations;
	}

}
