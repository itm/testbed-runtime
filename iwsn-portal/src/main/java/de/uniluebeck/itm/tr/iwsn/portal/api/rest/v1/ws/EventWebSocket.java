package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.ws;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.DevicesAttachedMessage;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.DevicesDetachedMessage;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.toJSON;

public class EventWebSocket implements WebSocket, WebSocket.OnTextMessage {

	private static final Logger log = LoggerFactory.getLogger(EventWebSocket.class);

	private final SchedulerService schedulerService;

	private final String remoteAddress;

	private final PortalEventBus portalEventBus;

	private Connection connection;

	private ScheduledFuture<?> keepAliveSchedule;

	@Inject
	public EventWebSocket(final PortalEventBus portalEventBus,
						  final SchedulerService schedulerService,
						  @Assisted final String remoteAddress) {
		this.schedulerService = schedulerService;
		this.remoteAddress = remoteAddress;
		this.portalEventBus = portalEventBus;
	}

	@Override
	public void onMessage(final String data) {
		// ignore
	}

	@Subscribe
	public void onDevicesAttachedEvent(final DevicesAttachedEvent event) {
		sendMessage(toJSON(new DevicesAttachedMessage(event)));
	}

	@Subscribe
	public void onDevicesDetachedEvent(final DevicesDetachedEvent event) {
		sendMessage(toJSON(new DevicesDetachedMessage(event)));
	}

	@Override
	public void onOpen(final Connection connection) {

		if (log.isTraceEnabled()) {
			log.trace("EventWebSocket connection opened: {}", connection);
		}

		this.connection = connection;
		this.portalEventBus.register(this);

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
			log.trace("EventWebSocket connection closed with code {} and message \"{}\": {}",
					closeCode,
					message,
					connection
			);
		}

		this.portalEventBus.unregister(this);
		this.connection = null;
	}

	@Override
	public String toString() {
		return "EventWebSocket[" + remoteAddress + "]@" + Integer.toHexString(hashCode());
	}

	private void sendMessage(final String data) {
		try {
			if (connection != null) {
				connection.sendMessage(data);
			} else {
				log.warn("Trying to send message over closed WebSocket!");
			}
		} catch (IOException e) {
			log.error("IOException while sending message over WebSocket connection " + connection);
		}
	}
}
