package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MessageWrapper extends OneToOneEncoder {

	private static final Map<Class, Function<MessageLite, Message>> map = new HashMap<>();

	static {

		// REQUESTS

		map.put(AreNodesAliveRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ARE_NODES_ALIVE)
				.setAreNodesAliveRequest((AreNodesAliveRequest) req)
				.build());

		map.put(AreNodesConnectedRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ARE_NODES_CONNECTED)
				.setAreNodesConnectedRequest((AreNodesConnectedRequest) req)
				.build());

		map.put(DisableNodesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_DISABLE_NODES)
				.setDisableNodesRequest((DisableNodesRequest) req)
				.build());

		map.put(DisableVirtualLinksRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_DISABLE_VIRTUAL_LINKS)
				.setDisableVirtualLinksRequest((DisableVirtualLinksRequest) req)
				.build());

		map.put(DisablePhysicalLinksRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_DISABLE_PHYSICAL_LINKS)
				.setDisablePhysicalLinksRequest((DisablePhysicalLinksRequest) req)
				.build());

		map.put(EnableNodesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ENABLE_NODES)
				.setEnableNodesRequest((EnableNodesRequest) req)
				.build());

		map.put(EnablePhysicalLinksRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ENABLE_PHYSICAL_LINKS)
				.setEnablePhysicalLinksRequest((EnablePhysicalLinksRequest) req)
				.build());

		map.put(EnableVirtualLinksRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_ENABLE_VIRTUAL_LINKS)
				.setEnableVirtualLinksRequest((EnableVirtualLinksRequest) req)
				.build());

		map.put(FlashImagesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_FLASH_IMAGES)
				.setFlashImagesRequest((FlashImagesRequest) req)
				.build());

		map.put(GetChannelPipelinesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_GET_CHANNEL_PIPELINES)
				.setGetChannelPipelinesRequest((GetChannelPipelinesRequest) req)
				.build());

		map.put(ResetNodesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_RESET_NODES)
				.setResetNodesRequest((ResetNodesRequest) req)
				.build());

		map.put(SendDownstreamMessagesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_SEND_DOWNSTREAM_MESSAGES)
				.setSendDownstreamMessagesRequest((SendDownstreamMessagesRequest) req)
				.build());

		map.put(SetChannelPipelinesRequest.class, req -> Message.newBuilder()
				.setType(MessageType.REQUEST_SET_CHANNEL_PIPELINES)
				.setSetChannelPipelinesRequest((SetChannelPipelinesRequest) req)
				.build());

		// RESPONSES

		map.put(Progress.class, prog -> Message.newBuilder()
				.setType(MessageType.PROGRESS)
				.setProgress((Progress) prog)
				.build());

		map.put(Response.class, resp -> Message.newBuilder()
				.setType(MessageType.RESPONSE)
				.setResponse((Response) resp)
				.build());

		map.put(GetChannelPipelinesResponse.class, resp -> Message.newBuilder()
				.setType(MessageType.GET_CHANNELPIPELINES_RESPONSE)
				.setGetChannelPipelinesResponse((GetChannelPipelinesResponse) resp)
				.build());

		// EVENTS

		map.put(UpstreamMessageEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_UPSTREAM_MESSAGE)
				.setUpstreamMessageEvent((UpstreamMessageEvent) event)
				.build());

		map.put(DevicesAttachedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICES_ATTACHED)
				.setDevicesAttachedEvent((DevicesAttachedEvent) event)
				.build());

		map.put(DevicesDetachedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICES_ATTACHED)
				.setDevicesDetachedEvent((DevicesDetachedEvent) event)
				.build());

		map.put(GatewayConnectedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_GATEWAY_CONNECTED)
				.setGatewayConnectedEvent((GatewayConnectedEvent) event)
				.build());

		map.put(GatewayDisconnectedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_GATEWAY_DISCONNECTED)
				.setGatewayDisconnectedEvent((GatewayDisconnectedEvent) event)
				.build());

		map.put(NotificationEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_NOTIFICATION)
				.setNotificationEvent((NotificationEvent) event)
				.build());

		map.put(ReservationStartedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_STARTED)
				.setReservationStartedEvent((ReservationStartedEvent) event)
				.build());

		map.put(ReservationEndedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_ENDED)
				.setReservationEndedEvent((ReservationEndedEvent) event)
				.build());

		map.put(ReservationMadeEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_MADE)
				.setReservationMadeEvent((ReservationMadeEvent) event)
				.build());

		map.put(ReservationCancelledEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_CANCELLED)
				.setReservationCancelledEvent((ReservationCancelledEvent) event)
				.build());

		map.put(ReservationOpenedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_OPENED)
				.setReservationOpenedEvent((ReservationOpenedEvent) event)
				.build());

		map.put(ReservationClosedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_CLOSED)
				.setReservationClosedEvent((ReservationClosedEvent) event)
				.build());

		map.put(ReservationFinalizedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_RESERVATION_FINALIZED)
				.setReservationFinalizedEvent((ReservationFinalizedEvent) event)
				.build());

		map.put(DeviceConfigCreatedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICE_CONFIG_CREATED)
				.setDeviceConfigCreatedEvent((DeviceConfigCreatedEvent) event)
				.build());

		map.put(DeviceConfigUpdatedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICE_CONFIG_UPDATED)
				.setDeviceConfigUpdatedEvent((DeviceConfigUpdatedEvent) event)
				.build());

		map.put(DeviceConfigDeletedEvent.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_DEVICE_CONFIG_DELETED)
				.setDeviceConfigDeletedEvent((DeviceConfigDeletedEvent) event)
				.build());

		map.put(EventAck.class, event -> Message.newBuilder()
				.setType(MessageType.EVENT_ACK)
				.setEventAck((EventAck) event)
				.build());
	}

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

		if (!(msg instanceof EventMessageEvent) && !(msg instanceof RequestResponseMessageEvent)) {
			throw new IllegalArgumentException("MessageWrapper handler only consumes events, requests and response " +
					"message events. Pipeline seems to be misconfigured.");
		}

		final Class<?> msgClass;
		if (msg instanceof EventMessageEvent) {
			msgClass = ((EventMessageEvent) msg).message.getClass();
		} else {
			msgClass = ((RequestResponseMessageEvent) msg).message.getClass();
		}

		final Function<MessageLite, Message> wrapperFunction = map.get(msgClass);

		if (wrapperFunction == null) {
			throw new IllegalArgumentException("Unknown message type \"" + msgClass + "\"");
		}

		return wrapperFunction.apply((MessageLite) msg);
	}
}
