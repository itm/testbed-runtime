package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import com.google.inject.assistedinject.Assisted;

public interface WsnWebSocketFactory {

	WsnWebSocket create(@Assisted("secretReservationKeyBase64") String secretReservationKeyBase64,
						@Assisted("remoteAddress") final String remoteAddress);
}
