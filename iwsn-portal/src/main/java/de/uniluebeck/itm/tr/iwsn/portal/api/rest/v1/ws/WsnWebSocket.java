package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.*;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.wisebed.api.v3.common.NodeUrn;
import org.eclipse.jetty.websocket.WebSocket;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.common.Base64Helper.decodeBytes;
import static de.uniluebeck.itm.tr.common.json.JSONHelper.fromJSON;
import static de.uniluebeck.itm.tr.common.json.JSONHelper.toJSON;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newSendDownstreamMessageRequest;

public class WsnWebSocket implements WebSocket, WebSocket.OnTextMessage {

	private static final Logger log = LoggerFactory.getLogger(WsnWebSocket.class);

	private final Reservation reservation;

	private final IdProvider requestIdProvider;

	private final SchedulerService schedulerService;

	private final String secretReservationKeysBase64;

	private final String remoteAddress;

	private Connection connection;

	private ScheduledFuture<?> keepAliveSchedule;

	@Inject
	public WsnWebSocket(final IdProvider requestIdProvider,
						final SchedulerService schedulerService,
						@Assisted final Reservation reservation,
						@Assisted("secretReservationKeysBase64") final String secretReservationKeysBase64,
						@Assisted("remoteAddress") final String remoteAddress) {
		log.trace("WsnWebSocket.WsnWebSocket()");
		this.requestIdProvider = requestIdProvider;
		this.schedulerService = schedulerService;
		this.secretReservationKeysBase64 = secretReservationKeysBase64;
		this.remoteAddress = remoteAddress;
		this.reservation = reservation;
	}

	@Override
	public void onMessage(final String data) {

		log.trace("WsnWebSocket.onMessage({})", data);

		try {
			final WebSocketDownstreamMessage message = fromJSON(data, WebSocketDownstreamMessage.class);
			byte[] decodedPayload = decodeBytes(message.payloadBase64);
			reservation.getEventBus().post(newSendDownstreamMessageRequest(
					secretReservationKeysBase64,
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

	@Subscribe
	public void onDevicesAttachedEvent(final DevicesAttachedEvent event) {
		sendMessage(toJSON(new DevicesAttachedMessage(event)));
	}

	@Subscribe
	public void onDevicesDetachedEvent(final DevicesDetachedEvent event) {
		sendMessage(toJSON(new DevicesDetachedMessage(event)));
	}

	@Subscribe
	public void onReservationStarted(final ReservationStartedEvent event) {
		if (!event.getSerializedKey().equals(reservation.getSerializedKey())) {
			throw new RuntimeException("This should not be possible!");
		}
		sendMessage(toJSON(new ReservationStartedMessage(reservation)));
	}

	@Subscribe
	public void onReservationEnded(final ReservationEndedEvent event) {
		if (!event.getSerializedKey().equals(reservation.getSerializedKey())) {
			throw new RuntimeException("This should not be possible!");
		}
		sendMessage(toJSON(new ReservationEndedMessage(reservation)));
	}

    @Subscribe
    public void onReservationCancelled(final ReservationCancelledEvent event) {
        if (!event.getSerializedKey().equals(reservation.getSerializedKey())) {
            throw new RuntimeException("This should not be possible!");
        }
        sendMessage(toJSON(new ReservationCancelledMessage(reservation)));
    }

	@Override
	public void onOpen(final Connection connection) {

		if (log.isTraceEnabled()) {
			log.trace("Websocket connection opened: {}", connection);
		}

		this.connection = connection;
		reservation.getEventBus().register(this);

		final DateTime start = reservation.getInterval().getStart();
		final DateTime end = reservation.getInterval().getEnd();

		if (end.isBeforeNow()) {
			sendMessage(toJSON(new ReservationEndedMessage(reservation)));
		} else if (start.isBeforeNow()) {
			sendMessage(toJSON(new ReservationStartedMessage(reservation)));
		}

		keepAliveSchedule = schedulerService.scheduleAtFixedRate(
				new WebSocketKeepAliveRunnable(connection), 60, 60, TimeUnit.SECONDS
		);
	}

	@Override
	public void onClose(final int closeCode, final String message) {

		if (keepAliveSchedule != null) {
			keepAliveSchedule.cancel(false);
			keepAliveSchedule = null;
		}

		if (log.isTraceEnabled()) {
			log.trace("Websocket connection closed with code {} and message \"{}\": {}", closeCode, message, connection
			);
		}

		reservation.getEventBus().unregister(this);
		this.connection = null;
	}

	@Override
	public String toString() {
		return "WsnWebSocket[" + remoteAddress + "]@" + Integer.toHexString(hashCode());
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
