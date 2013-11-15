package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;

public interface WsnWebSocketFactory {

	WsnWebSocket create(@Assisted Reservation reservation,
						@Assisted("secretReservationKeysBase64") String secretReservationKeysBase64,
						@Assisted("remoteAddress") String remoteAddress);
}
