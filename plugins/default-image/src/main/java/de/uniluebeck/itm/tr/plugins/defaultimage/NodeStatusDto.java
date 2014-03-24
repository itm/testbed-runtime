package de.uniluebeck.itm.tr.plugins.defaultimage;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NodeStatusDto {

	public String nodeUrn;

	public FlashStatus flashStatus;

	public ReservationStatus reservationStatus;

	public NodeStatusDto(final String nodeUrn, final FlashStatus flashStatus,
						 final ReservationStatus reservationStatus) {
		this.nodeUrn = nodeUrn;
		this.flashStatus = flashStatus;
		this.reservationStatus = reservationStatus;
	}

	public NodeStatusDto() {
	}
}
