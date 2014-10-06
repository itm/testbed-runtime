package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@XmlRootElement
public class ReservationFinalizedMessage {

	@XmlElement(name = "type")
	public final String type = "reservationFinalized";

	@XmlElement(name = "timestamp")
	public String timestamp;

	@XmlElement(name = "reservationData")
	public List<ConfidentialReservationData> reservationData;

	@SuppressWarnings("UnusedDeclaration")
	public ReservationFinalizedMessage() {
	}

	public ReservationFinalizedMessage(final Reservation reservation) {
		assert reservation.getFinalized() != null;
		this.timestamp = reservation.getFinalized().toString(ISODateTimeFormat.dateTime());
		this.reservationData = newArrayList(reservation.getConfidentialReservationData());
	}

	@Override
	public String toString() {
		return "ReservationFinalizedMessage{" +
				"type='" + type + '\'' +
				", timestamp='" + timestamp + '\'' +
				", reservationData=" + reservationData +
				'}';
	}
}
