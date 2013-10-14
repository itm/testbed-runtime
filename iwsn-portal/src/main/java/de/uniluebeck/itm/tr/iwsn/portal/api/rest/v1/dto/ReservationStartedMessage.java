package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReservationStartedMessage {

	@XmlElement(name = "type")
	public final String type = "reservationStarted";

	@XmlElement(name = "timestamp")
	public String timestamp;

	@XmlElement(name = "reservationData")
	public ConfidentialReservationDataList reservationData;

	@SuppressWarnings("unused")
	public ReservationStartedMessage() {
	}

	public ReservationStartedMessage(final Reservation reservation) {
		this.timestamp = reservation.getInterval().getStart().toString(ISODateTimeFormat.dateTime());
		this.reservationData = new ConfidentialReservationDataList(reservation.getConfidentialReservationData());
	}

	@Override
	public String toString() {
		return "ReservationStartedMessage{" +
				"type='" + type + '\'' +
				", timestamp='" + timestamp + '\'' +
				", reservationData=" + reservationData +
				'}';
	}
}
