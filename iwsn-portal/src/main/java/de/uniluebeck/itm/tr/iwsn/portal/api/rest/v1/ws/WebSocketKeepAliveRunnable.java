package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.KeepAliveMessage;
import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.toJSON;

public class WebSocketKeepAliveRunnable implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(WebSocketKeepAliveRunnable.class);

	private final WebSocket.Connection connection;

	public WebSocketKeepAliveRunnable(final WebSocket.Connection connection) {
		this.connection = connection;
	}

	@Override
	public void run() {
		log.trace("WebSocketKeepAliveRunnable.run()");
		try {
			connection.sendMessage(toJSON(new KeepAliveMessage()));
		} catch (Exception e) {
			log.warn("Exception while sending WebSocket keepalive message: ", e);
		}
	}
}
