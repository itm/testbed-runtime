package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeepAliveRunnable implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(KeepAliveRunnable.class);

	private final WebSocket.Connection connection;

	public KeepAliveRunnable(final WebSocket.Connection connection) {
		this.connection = connection;
	}

	@Override
	public void run() {
		log.trace("KeepAliveRunnable.run()");
		try {
			connection.sendMessage("{}");
		} catch (Exception e) {
			log.warn("Exception while sending WebSocket keepalive message: ", e);
		}
	}
}
