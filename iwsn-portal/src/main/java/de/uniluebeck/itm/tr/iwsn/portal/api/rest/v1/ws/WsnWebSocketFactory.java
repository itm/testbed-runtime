package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

public interface WsnWebSocketFactory {

	WsnWebSocket create(String experimentUrl);
}
