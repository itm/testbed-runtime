package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.WebSocketDownstreamMessage;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.WebSocketNotificationMessage;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.WebSocketUpstreamMessage;
import eu.wisebed.api.v3.common.NodeUrn;
import org.eclipse.jetty.websocket.WebSocket;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newSendDownstreamMessageRequest;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper.decodeBytes;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.fromJSON;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.toJSON;

public class WsnWebSocket implements WebSocket, WebSocket.OnTextMessage {

	private static final Logger log = LoggerFactory.getLogger(WsnWebSocket.class);

	private final Reservation reservation;

	private final RequestIdProvider requestIdProvider;

	private Connection connection;

	@Inject
	public WsnWebSocket(final RequestIdProvider requestIdProvider,
						final ReservationManager reservationManager,
						@Assisted final String secretReservationKeyBase64) {
		this.requestIdProvider = requestIdProvider;
		try {
			this.reservation = reservationManager.getReservation(secretReservationKeyBase64);
		} catch (ReservationUnknownException e) {
			throw propagate(e);
		}
	}

	@Override
	public void onMessage(final String data) {

		try {
			final WebSocketDownstreamMessage message = fromJSON(data, WebSocketDownstreamMessage.class);
			byte[] decodedPayload = decodeBytes(message.payloadBase64);
			reservation.getEventBus().post(newSendDownstreamMessageRequest(
					reservation.getKey(),
					requestIdProvider.get(),
					newArrayList(new NodeUrn(message.targetNodeUrn)),
					decodedPayload
			)
			);
		} catch (Exception e) {
			final WebSocketNotificationMessage notificationMessage = new WebSocketNotificationMessage(
					DateTime.now(),
					null,
					"The following downstream message could not be parsed by the server: " + data + ". Exception: " + e
			);
			sendMessage(toJSON(notificationMessage));
		}
	}

	@Subscribe
	public void onUpstreamMessageEvent(final UpstreamMessageEvent event) {
		sendMessage(toJSON(new WebSocketUpstreamMessage(event)));
	}

	@Subscribe
	public void onNotificationsEvent(final NotificationEvent event) {
		sendMessage(toJSON(new WebSocketNotificationMessage(event)));
	}

	@Override
	public void onOpen(final Connection connection) {

		if (log.isInfoEnabled()) {
			log.info("Websocket connection opened: {}", connection);
		}

		this.connection = connection;
		reservation.getEventBus().register(this);
	}

	@Override
	public void onClose(final int closeCode, final String message) {

		if (log.isInfoEnabled()) {
			log.info("Websocket connection closed with code {} and message \"{}\": {}", closeCode, message, connection);
		}

		reservation.getEventBus().unregister(this);
		this.connection = null;
	}

	private void sendMessage(final String data) {
		try {
			if (connection != null) {
				connection.sendMessage(data);
			} else {
				log.warn("Trying to send message over closed WebSocket!");
			}
		} catch (IOException e) {
			log.error("IOException while sending message over websocket connection " + connection);
		}
	}
}
