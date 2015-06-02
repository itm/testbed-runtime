package de.uniluebeck.itm.tr.iwsn.common;

import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MessageWrapper extends OneToOneEncoder {

	private static final Map<Class, Function<MessageLite, Message>> MAP = new HashMap<>();

	static {

		// REQUESTS

		MAP.put(AreNodesAliveRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ARE_NODES_ALIVE)
				.setAreNodesAliveRequest((AreNodesAliveRequest) req)
				.build());

		MAP.put(AreNodesConnectedRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ARE_NODES_CONNECTED)
				.setAreNodesConnectedRequest((AreNodesConnectedRequest) req)
				.build());

		MAP.put(DisableNodesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_DISABLE_NODES)
				.setDisableNodesRequest((DisableNodesRequest) req)
				.build());

		MAP.put(DisableVirtualLinksRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_DISABLE_VIRTUAL_LINKS)
				.setDisableVirtualLinksRequest((DisableVirtualLinksRequest) req)
				.build());

		MAP.put(DisablePhysicalLinksRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_DISABLE_PHYSICAL_LINKS)
				.setDisablePhysicalLinksRequest((DisablePhysicalLinksRequest) req)
				.build());

		MAP.put(EnableNodesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ENABLE_NODES)
				.setEnableNodesRequest((EnableNodesRequest) req)
				.build());

		MAP.put(EnablePhysicalLinksRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ENABLE_PHYSICAL_LINKS)
				.setEnablePhysicalLinksRequest((EnablePhysicalLinksRequest) req)
				.build());

		MAP.put(EnableVirtualLinksRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ENABLE_VIRTUAL_LINKS)
				.setEnableVirtualLinksRequest((EnableVirtualLinksRequest) req)
				.build());

		MAP.put(FlashImagesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_FLASH_IMAGES)
				.setFlashImagesRequest((FlashImagesRequest) req)
				.build());

		MAP.put(GetChannelPipelinesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_GET_CHANNEL_PIPELINES)
				.setGetChannelPipelinesRequest((GetChannelPipelinesRequest) req)
				.build());

		MAP.put(ResetNodesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_RESET_NODES)
				.setResetNodesRequest((ResetNodesRequest) req)
				.build());

		MAP.put(SendDownstreamMessagesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_SEND_DOWNSTREAM_MESSAGES)
				.setSendDownstreamMessagesRequest((SendDownstreamMessagesRequest) req)
				.build());

		MAP.put(SetChannelPipelinesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_SET_CHANNEL_PIPELINES)
				.setSetChannelPipelinesRequest((SetChannelPipelinesRequest) req)
				.build());

		// RESPONSES

		MAP.put(Progress.class, prog -> Message.newBuilder()
				.setType(MessageType.PROGRESS)
				.setProgress((Progress) prog)
				.build());

		MAP.put(Response.class, resp -> Message.newBuilder()
				.setType(MessageType.RESPONSE)
				.setResponse((Response) resp)
				.build());

		MAP.put(GetChannelPipelinesResponse.class, resp -> Message.newBuilder()
				.setType(MessageType.RESPONSE_GET_CHANNELPIPELINES)
				.setGetChannelPipelinesResponse((GetChannelPipelinesResponse) resp)
				.build());

		// EVENTS

		MAP.put(UpstreamMessageEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_UPSTREAM_MESSAGE)
				.setUpstreamMessageEvent((UpstreamMessageEvent) event)
				.build());

		MAP.put(DevicesAttachedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICES_ATTACHED)
				.setDevicesAttachedEvent((DevicesAttachedEvent) event)
				.build());

		MAP.put(DevicesDetachedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICES_ATTACHED)
				.setDevicesDetachedEvent((DevicesDetachedEvent) event)
				.build());

		MAP.put(GatewayConnectedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_GATEWAY_CONNECTED)
				.setGatewayConnectedEvent((GatewayConnectedEvent) event)
				.build());

		MAP.put(GatewayDisconnectedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_GATEWAY_DISCONNECTED)
				.setGatewayDisconnectedEvent((GatewayDisconnectedEvent) event)
				.build());

		MAP.put(NotificationEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_NOTIFICATION)
				.setNotificationEvent((NotificationEvent) event)
				.build());

		MAP.put(ReservationStartedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_STARTED)
				.setReservationStartedEvent((ReservationStartedEvent) event)
				.build());

		MAP.put(ReservationEndedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_ENDED)
				.setReservationEndedEvent((ReservationEndedEvent) event)
				.build());

		MAP.put(ReservationMadeEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_MADE)
				.setReservationMadeEvent((ReservationMadeEvent) event)
				.build());

		MAP.put(ReservationCancelledEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_CANCELLED)
				.setReservationCancelledEvent((ReservationCancelledEvent) event)
				.build());

		MAP.put(ReservationOpenedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_OPENED)
				.setReservationOpenedEvent((ReservationOpenedEvent) event)
				.build());

		MAP.put(ReservationClosedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_CLOSED)
				.setReservationClosedEvent((ReservationClosedEvent) event)
				.build());

		MAP.put(ReservationFinalizedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_FINALIZED)
				.setReservationFinalizedEvent((ReservationFinalizedEvent) event)
				.build());

		MAP.put(DeviceConfigCreatedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICE_CONFIG_CREATED)
				.setDeviceConfigCreatedEvent((DeviceConfigCreatedEvent) event)
				.build());

		MAP.put(DeviceConfigUpdatedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICE_CONFIG_UPDATED)
				.setDeviceConfigUpdatedEvent((DeviceConfigUpdatedEvent) event)
				.build());

		MAP.put(DeviceConfigDeletedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICE_CONFIG_DELETED)
				.setDeviceConfigDeletedEvent((DeviceConfigDeletedEvent) event)
				.build());

		MAP.put(EventAck.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_ACK)
				.setEventAck((EventAck) event)
				.build());
	}

	public static final Function<MessageLite, Message> WRAP_FUNCTION = (msg) -> {

		final Function<MessageLite, Message> wrapperFunction = MAP.get(msg.getClass());

		if (wrapperFunction == null) {
			throw new IllegalArgumentException("Unknown message type \"" + msg.getClass() + "\"");
		}

		return wrapperFunction.apply(msg);
	};

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

		if (!(msg instanceof MessageHeaderPair)) {
			throw new IllegalArgumentException("MessageWrapper handler only consumes events, requests and response " +
					"message events. Pipeline seems to be misconfigured.");
		}

		return WRAP_FUNCTION.apply(((MessageHeaderPair) msg).message);
	}

	public static Message wrap(MessageLite message) {
		return WRAP_FUNCTION.apply(message);
	}
}
