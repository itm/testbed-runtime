package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@XmlRootElement
public class ReservationStartedMessage {

	@XmlElement(name = "type")
	public final String type = "reservationStarted";

	@XmlElement(name = "timestamp")
	public String timestamp;

	@XmlElement(name = "reservationData")
	public List<ConfidentialReservationData> reservationData;

	@SuppressWarnings("unused")
	public ReservationStartedMessage() {
	}

	public ReservationStartedMessage(final Reservation reservation) {
		this.timestamp = reservation.getInterval().getStart().toString(ISODateTimeFormat.dateTime());
		this.reservationData = newArrayList(reservation.getConfidentialReservationData());
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
