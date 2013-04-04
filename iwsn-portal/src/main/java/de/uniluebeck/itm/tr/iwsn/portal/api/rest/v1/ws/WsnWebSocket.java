package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.jersey.core.util.Base64;
import eu.wisebed.restws.dto.WebSocketDownstreamMessage;
import eu.wisebed.restws.dto.WebSocketNotificationMessage;
import eu.wisebed.restws.dto.WebSocketUpstreamMessage;
import eu.wisebed.restws.event.DownstreamMessageEvent;
import eu.wisebed.restws.event.NotificationsEvent;
import eu.wisebed.restws.event.UpstreamMessageEvent;
import eu.wisebed.restws.proxy.WsnProxyManagerService;
import eu.wisebed.restws.util.InjectLogger;
import eu.wisebed.restws.util.JSONHelper;
import org.eclipse.jetty.websocket.WebSocket;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import java.io.IOException;

public class WsnWebSocket implements WebSocket, WebSocket.OnTextMessage {

	@InjectLogger
	private Logger log;

	private final WsnProxyManagerService wsnProxyManagerService;

	private final String experimentWsnInstanceEndpointUrl;

	private Connection connection;

	@Inject
	public WsnWebSocket(final WsnProxyManagerService wsnProxyManagerService,
						@Assisted final String experimentWsnInstanceEndpointUrl) {
		this.wsnProxyManagerService = wsnProxyManagerService;
		this.experimentWsnInstanceEndpointUrl = experimentWsnInstanceEndpointUrl;
	}

	@Override
	public void onMessage(final String data) {
		try {
			WebSocketDownstreamMessage message = JSONHelper.fromJSON(data, WebSocketDownstreamMessage.class);
			byte[] decodedPayload = Base64.decode(message.payloadBase64);
			DownstreamMessageEvent downstreamMessageEvent =
					new DownstreamMessageEvent(new DateTime(), message.targetNodeUrn, decodedPayload);
			log.debug("Sending message downstream to \"{}\", message: {}", message.targetNodeUrn, decodedPayload);
			wsnProxyManagerService.getEventBus(experimentWsnInstanceEndpointUrl).post(downstreamMessageEvent);
		} catch (Exception e) {
			sendNotification(
					new DateTime(),
					"The following downstream message could not be parsed by the server: " + data + ". Exception: " + e
			);
		}
	}

	@Subscribe
	public void onUpstreamMessageEvent(final UpstreamMessageEvent message) {
		sendUpstream(message.getTimestamp(), message.getSourceNodeUrn(), message.getMessageBytes());
	}

	@Subscribe
	public void onNotificationsEvent(final NotificationsEvent notifications) {
		for (String notification : notifications.getNotifications()) {
			sendNotification(new DateTime(), notification);
		}
	}

	@Override
	public void onOpen(final Connection connection) {

		log.info("Websocket connection opened: {}", connection);

		this.connection = connection;
		getEventBus(experimentWsnInstanceEndpointUrl).register(this);
	}

	@Override
	public void onClose(final int closeCode, final String message) {

		if (log.isInfoEnabled()) {
			log.info("Websocket connection closed with code {} and message \"{}\": {}",
					new Object[]{closeCode, message, connection}
			);
		}

		getEventBus(experimentWsnInstanceEndpointUrl).unregister(this);
		this.connection = null;
	}

	private void sendUpstream(final DateTime dateTime, final String sourceNode, final byte[] payloadBytes) {
		WebSocketUpstreamMessage upstreamMessage = new WebSocketUpstreamMessage(
				dateTime.toString(ISODateTimeFormat.basicDateTime()),
				sourceNode,
				new String(Base64.encode(payloadBytes))
		);
		String json;
		try {
			json = JSONHelper.toJSON(upstreamMessage);
		} catch (Exception e) {
			log.error("", e);
			JSONHelper.toJSON(upstreamMessage);
			return;
		}
		log.trace("Sending upstream message via WebSocket: ", json);
		sendMessage(json);
	}

	private void sendNotification(final DateTime dateTime, final String notificationString) {
		WebSocketNotificationMessage notification = new WebSocketNotificationMessage(
				dateTime.toString(ISODateTimeFormat.basicDateTime()),
				notificationString
		);
		sendMessage(JSONHelper.toJSON(notification));
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

	private EventBus getEventBus(final String experimentWsnInstanceEndpointUrl) {
		return wsnProxyManagerService.getEventBus(experimentWsnInstanceEndpointUrl);
	}
}
