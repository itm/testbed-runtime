package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.rs.PublicReservationData;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class PublicReservationDataList {

	public List<PublicReservationData> reservations;

	public PublicReservationDataList() {
	}

	public PublicReservationDataList(List<PublicReservationData> reservations) {
		this.reservations = reservations;
	}

}
