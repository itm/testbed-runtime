package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

public interface EventWebSocketFactory {

	EventWebSocket create(final String remoteAddress);

}
