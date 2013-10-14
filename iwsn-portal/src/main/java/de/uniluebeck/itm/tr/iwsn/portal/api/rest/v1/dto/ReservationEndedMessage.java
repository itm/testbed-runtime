package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReservationEndedMessage {

	@XmlElement(name = "type")
	public final String type = "reservationEnded";

	@XmlElement(name = "timestamp")
	public String timestamp;

	@XmlElement(name = "reservationData")
	public ConfidentialReservationDataList reservationData;

	@SuppressWarnings("unused")
	public ReservationEndedMessage() {
	}

	public ReservationEndedMessage(final Reservation reservation) {
		this.timestamp = reservation.getInterval().getEnd().toString(ISODateTimeFormat.dateTime());
		this.reservationData = new ConfidentialReservationDataList(reservation.getConfidentialReservationData());
	}

	@Override
	public String toString() {
		return "ReservationEndedMessage{" +
				"type='" + type + '\'' +
				", timestamp='" + timestamp + '\'' +
				", reservationData=" + reservationData +
				'}';
	}
}
