package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReservationStartedMessage {

	@XmlElement(name = "type")
	public final String type = "reservationStarted";

	@XmlElement(name = "timestamp")
	public String timestamp;

	@SuppressWarnings("unused")
	public ReservationStartedMessage() {
	}

	public ReservationStartedMessage(final DateTime timestamp) {
		this.timestamp = timestamp.toString(ISODateTimeFormat.dateTime());
	}

	public ReservationStartedMessage(final ReservationStartedEvent event) {
		this(event.getReservation().getInterval().getStart());
	}

	@Override
	public String toString() {
		return "ReservationStartedMessage{" +
				"type='" + type + '\'' +
				", timestamp='" + timestamp + '\'' +
				"} " + super.toString();
	}
}
