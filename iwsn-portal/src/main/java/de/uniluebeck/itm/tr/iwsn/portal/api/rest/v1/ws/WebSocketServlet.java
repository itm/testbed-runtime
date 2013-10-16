package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;
import org.eclipse.jetty.websocket.WebSocket;

import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

@Singleton
@ThreadSafe
public class WebSocketServlet extends org.eclipse.jetty.websocket.WebSocketServlet {

	private static final long serialVersionUID = 160812781199698413L;

	@Inject
	private WsnWebSocketFactory wsnWebSocketFactory;

	@Inject
	private EventWebSocketFactory eventWebSocketFactory;

	@Inject
	private ReservationManager reservationManager;

	@Override
	public WebSocket doWebSocketConnect(final HttpServletRequest request, final String protocol) {

		final String remoteAddress = request.getRemoteAddr() + ":" + request.getRemotePort();
		final String uriString = request.getRequestURI();
		final URI requestUri;
		try {
			requestUri = new URI(uriString);
		} catch (URISyntaxException e) {
			return null;
		}

		if (requestUri.getPath().endsWith("events")) {
			return eventWebSocketFactory.create(remoteAddress);
		}

		String path = requestUri.getPath().startsWith("/") ? requestUri.getPath().substring(1) : requestUri.getPath();
		String[] splitPath = path.split("/");

		try {

			final String secretReservationKeysBase64 = splitPath[splitPath.length - 1];
			final Reservation reservation = reservationManager.getReservation(secretReservationKeysBase64);
			return wsnWebSocketFactory.create(reservation, secretReservationKeysBase64, remoteAddress);

		} catch (ReservationUnknownException e) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}
}
