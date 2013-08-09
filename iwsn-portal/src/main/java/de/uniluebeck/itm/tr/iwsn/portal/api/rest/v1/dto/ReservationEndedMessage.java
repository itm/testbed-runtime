package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReservationEndedMessage {

	@XmlElement(name = "type")
	public final String type = "reservationEnded";

	@XmlElement(name = "timestamp")
	public String timestamp;

	@SuppressWarnings("unused")
	public ReservationEndedMessage() {
	}

	public ReservationEndedMessage(final DateTime timestamp) {
		this.timestamp = timestamp.toString(ISODateTimeFormat.dateTime());
	}

	public ReservationEndedMessage(final ReservationEndedEvent event) {
		this(event.getReservation().getInterval().getStart());
	}

	@Override
	public String toString() {
		return "ReservationEndedMessage{" +
				"type='" + type + '\'' +
				", timestamp='" + timestamp + '\'' +
				"} " + super.toString();
	}
}
