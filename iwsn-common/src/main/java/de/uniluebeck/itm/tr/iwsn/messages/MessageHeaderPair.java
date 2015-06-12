package de.uniluebeck.itm.tr.iwsn.messages;

import com.google.protobuf.MessageLite;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;

public class MessageHeaderPair {

	public static final Set<Class> MESSAGE_CLASS_TYPES = new HashSet<>();

	public static final Set<MessageType> MESSAGE_TYPES = new HashSet<>();

	public static final Set<MessageType> EVENT_TYPES = new HashSet<>();

	public static final Set<MessageType> SCOPEABLE_EVENT_TYPES = new HashSet<>();

	public static final Set<MessageType> REQUEST_TYPES = new HashSet<>();

	public static final Set<MessageType> RESPONSE_TYPES = new HashSet<>();

	public static final Set<MessageType> RESERVATION_EVENT_TYPES = new HashSet<>();

	static {

		// REQUESTS
		MESSAGE_CLASS_TYPES.add(AreNodesAliveRequest.class);
		MESSAGE_CLASS_TYPES.add(AreNodesConnectedRequest.class);
		MESSAGE_CLASS_TYPES.add(DisableNodesRequest.class);
		MESSAGE_CLASS_TYPES.add(DisableVirtualLinksRequest.class);
		MESSAGE_CLASS_TYPES.add(DisablePhysicalLinksRequest.class);
		MESSAGE_CLASS_TYPES.add(EnableNodesRequest.class);
		MESSAGE_CLASS_TYPES.add(EnablePhysicalLinksRequest.class);
		MESSAGE_CLASS_TYPES.add(EnableVirtualLinksRequest.class);
		MESSAGE_CLASS_TYPES.add(FlashImagesRequest.class);
		MESSAGE_CLASS_TYPES.add(GetChannelPipelinesRequest.class);
		MESSAGE_CLASS_TYPES.add(ResetNodesRequest.class);
		MESSAGE_CLASS_TYPES.add(SendDownstreamMessagesRequest.class);
		MESSAGE_CLASS_TYPES.add(SetChannelPipelinesRequest.class);

		// RESPONSES
		MESSAGE_CLASS_TYPES.add(Progress.class);
		MESSAGE_CLASS_TYPES.add(Response.class);
		MESSAGE_CLASS_TYPES.add(GetChannelPipelinesResponse.class);

		// EVENTS
		MESSAGE_CLASS_TYPES.add(UpstreamMessageEvent.class);
		MESSAGE_CLASS_TYPES.add(DevicesAttachedEvent.class);
		MESSAGE_CLASS_TYPES.add(DevicesDetachedEvent.class);
		MESSAGE_CLASS_TYPES.add(GatewayConnectedEvent.class);
		MESSAGE_CLASS_TYPES.add(GatewayDisconnectedEvent.class);
		MESSAGE_CLASS_TYPES.add(NotificationEvent.class);
		MESSAGE_CLASS_TYPES.add(ReservationStartedEvent.class);
		MESSAGE_CLASS_TYPES.add(ReservationEndedEvent.class);
		MESSAGE_CLASS_TYPES.add(ReservationMadeEvent.class);
		MESSAGE_CLASS_TYPES.add(ReservationCancelledEvent.class);
		MESSAGE_CLASS_TYPES.add(ReservationOpenedEvent.class);
		MESSAGE_CLASS_TYPES.add(ReservationClosedEvent.class);
		MESSAGE_CLASS_TYPES.add(ReservationFinalizedEvent.class);
		MESSAGE_CLASS_TYPES.add(DeviceConfigCreatedEvent.class);
		MESSAGE_CLASS_TYPES.add(DeviceConfigUpdatedEvent.class);
		MESSAGE_CLASS_TYPES.add(DeviceConfigDeletedEvent.class);
		MESSAGE_CLASS_TYPES.add(EventAck.class);

		// EVENT TYPES
		EVENT_TYPES.add(MessageType.EVENT_ACK);
		EVENT_TYPES.add(MessageType.EVENT_DEVICE_CONFIG_CREATED);
		EVENT_TYPES.add(MessageType.EVENT_DEVICE_CONFIG_DELETED);
		EVENT_TYPES.add(MessageType.EVENT_DEVICE_CONFIG_UPDATED);
		EVENT_TYPES.add(MessageType.EVENT_DEVICES_ATTACHED);
		EVENT_TYPES.add(MessageType.EVENT_DEVICES_DETACHED);
		EVENT_TYPES.add(MessageType.EVENT_GATEWAY_CONNECTED);
		EVENT_TYPES.add(MessageType.EVENT_GATEWAY_DISCONNECTED);
		EVENT_TYPES.add(MessageType.EVENT_NOTIFICATION);
		EVENT_TYPES.add(MessageType.EVENT_RESERVATION_CANCELLED);
		EVENT_TYPES.add(MessageType.EVENT_RESERVATION_CLOSED);
		EVENT_TYPES.add(MessageType.EVENT_RESERVATION_ENDED);
		EVENT_TYPES.add(MessageType.EVENT_RESERVATION_FINALIZED);
		EVENT_TYPES.add(MessageType.EVENT_RESERVATION_MADE);
		EVENT_TYPES.add(MessageType.EVENT_RESERVATION_OPENED);
		EVENT_TYPES.add(MessageType.EVENT_RESERVATION_STARTED);
		EVENT_TYPES.add(MessageType.EVENT_UPSTREAM_MESSAGE);

		// SCOPEABLE EVENT TYPES
		SCOPEABLE_EVENT_TYPES.add(MessageType.EVENT_DEVICES_ATTACHED);
		SCOPEABLE_EVENT_TYPES.add(MessageType.EVENT_DEVICES_DETACHED);
		SCOPEABLE_EVENT_TYPES.add(MessageType.EVENT_NOTIFICATION);

		// REQUEST TYPES
		REQUEST_TYPES.add(MessageType.REQUEST_ARE_NODES_ALIVE);
		REQUEST_TYPES.add(MessageType.REQUEST_ARE_NODES_CONNECTED);
		REQUEST_TYPES.add(MessageType.REQUEST_DISABLE_NODES);
		REQUEST_TYPES.add(MessageType.REQUEST_DISABLE_PHYSICAL_LINKS);
		REQUEST_TYPES.add(MessageType.REQUEST_DISABLE_VIRTUAL_LINKS);
		REQUEST_TYPES.add(MessageType.REQUEST_ENABLE_NODES);
		REQUEST_TYPES.add(MessageType.REQUEST_ENABLE_PHYSICAL_LINKS);
		REQUEST_TYPES.add(MessageType.REQUEST_ENABLE_VIRTUAL_LINKS);
		REQUEST_TYPES.add(MessageType.REQUEST_FLASH_IMAGES);
		REQUEST_TYPES.add(MessageType.REQUEST_GET_CHANNEL_PIPELINES);
		REQUEST_TYPES.add(MessageType.REQUEST_RESET_NODES);
		REQUEST_TYPES.add(MessageType.REQUEST_SEND_DOWNSTREAM_MESSAGES);
		REQUEST_TYPES.add(MessageType.REQUEST_SET_CHANNEL_PIPELINES);

		// RESPONSE TYPES
		RESPONSE_TYPES.add(MessageType.PROGRESS);
		RESPONSE_TYPES.add(MessageType.RESPONSE);
		RESPONSE_TYPES.add(MessageType.RESPONSE_GET_CHANNELPIPELINES);

		// RESERVATION EVENT TYPES
		RESERVATION_EVENT_TYPES.add(MessageType.EVENT_RESERVATION_CANCELLED);
		RESERVATION_EVENT_TYPES.add(MessageType.EVENT_RESERVATION_CLOSED);
		RESERVATION_EVENT_TYPES.add(MessageType.EVENT_RESERVATION_ENDED);
		RESERVATION_EVENT_TYPES.add(MessageType.EVENT_RESERVATION_FINALIZED);
		RESERVATION_EVENT_TYPES.add(MessageType.EVENT_RESERVATION_MADE);
		RESERVATION_EVENT_TYPES.add(MessageType.EVENT_RESERVATION_OPENED);
		RESERVATION_EVENT_TYPES.add(MessageType.EVENT_RESERVATION_STARTED);

		// ALL MESSAGE TYPES
		MESSAGE_TYPES.addAll(EVENT_TYPES);
		MESSAGE_TYPES.addAll(REQUEST_TYPES);
		MESSAGE_TYPES.addAll(RESPONSE_TYPES);
	}

	public final Header header;

	public final MessageLite message;

	public MessageHeaderPair(Header header, MessageLite unwrappedMessage) {
		this.header = header;
		this.message = unwrappedMessage;
	}

	public static boolean isUnwrappedMessageEvent(Object obj) {
		return MESSAGE_CLASS_TYPES.contains(obj.getClass());
	}

	public static boolean isWrappedMessageEvent(Object obj) {
		return obj instanceof Message;
	}

	public static boolean isEvent(MessageType type) {
		return EVENT_TYPES.contains(type);
	}

	public static boolean isRequest(MessageType type) {
		return REQUEST_TYPES.contains(type);
	}

	public static boolean isScopeableEvent(MessageType type) {
		return SCOPEABLE_EVENT_TYPES.contains(type);
	}

	public static boolean isResponse(MessageType type) {
		return RESPONSE_TYPES.contains(type);
	}

	public static boolean isReservationEvent(MessageType type) {
		return RESERVATION_EVENT_TYPES.contains(type);
	}

	public static MessageHeaderPair fromUnwrapped(Object obj) {

		if (!isUnwrappedMessageEvent(obj)) {
			throw new IllegalArgumentException("Unknown message type \"" + obj.getClass() + "\"!");
		}

		try {
			return new MessageHeaderPair(
					(Header) obj.getClass().getMethod("getHeader").invoke(obj),
					(MessageLite) obj
			);
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	public static MessageHeaderPair fromWrapped(Object obj) {

		if (!isWrappedMessageEvent(obj)) {
			throw new IllegalArgumentException("Unknown message type \"" + obj.getClass() + "\"!");
		}

		Message msg = (Message) obj;

		switch (msg.getType()) {

			// REQUESTS
			case REQUEST_ARE_NODES_ALIVE:
				return new MessageHeaderPair(
						msg.getAreNodesAliveRequest().getHeader(),
						msg.getAreNodesAliveRequest()
				);
			case REQUEST_ARE_NODES_CONNECTED:
				return new MessageHeaderPair(
						msg.getAreNodesConnectedRequest().getHeader(),
						msg.getAreNodesConnectedRequest()
				);
			case REQUEST_DISABLE_NODES:
				return new MessageHeaderPair(
						msg.getDisableNodesRequest().getHeader(),
						msg.getDisableNodesRequest()
				);
			case REQUEST_DISABLE_VIRTUAL_LINKS:
				return new MessageHeaderPair(
						msg.getDisableVirtualLinksRequest().getHeader(),
						msg.getDisableVirtualLinksRequest()
				);
			case REQUEST_DISABLE_PHYSICAL_LINKS:
				return new MessageHeaderPair(
						msg.getDisablePhysicalLinksRequest().getHeader(),
						msg.getDisablePhysicalLinksRequest()
				);
			case REQUEST_ENABLE_NODES:
				return new MessageHeaderPair(
						msg.getEnableNodesRequest().getHeader(),
						msg.getEnableNodesRequest()
				);
			case REQUEST_ENABLE_PHYSICAL_LINKS:
				return new MessageHeaderPair(
						msg.getEnablePhysicalLinksRequest().getHeader(),
						msg.getEnablePhysicalLinksRequest()
				);
			case REQUEST_ENABLE_VIRTUAL_LINKS:
				return new MessageHeaderPair(
						msg.getEnableVirtualLinksRequest().getHeader(),
						msg.getEnableVirtualLinksRequest()
				);
			case REQUEST_FLASH_IMAGES:
				return new MessageHeaderPair(
						msg.getFlashImagesRequest().getHeader(),
						msg.getFlashImagesRequest()
				);
			case REQUEST_GET_CHANNEL_PIPELINES:
				return new MessageHeaderPair(
						msg.getGetChannelPipelinesRequest().getHeader(),
						msg.getGetChannelPipelinesRequest()
				);
			case REQUEST_RESET_NODES:
				return new MessageHeaderPair(
						msg.getResetNodesRequest().getHeader(),
						msg.getResetNodesRequest()
				);
			case REQUEST_SEND_DOWNSTREAM_MESSAGES:
				return new MessageHeaderPair(
						msg.getSendDownstreamMessagesRequest().getHeader(),
						msg.getSendDownstreamMessagesRequest()
				);
			case REQUEST_SET_CHANNEL_PIPELINES:
				return new MessageHeaderPair(
						msg.getSetChannelPipelinesRequest().getHeader(),
						msg.getSetChannelPipelinesRequest()
				);

			// RESPONSES
			case PROGRESS:
				return new MessageHeaderPair(
						msg.getProgress().getHeader(),
						msg.getProgress()
				);
			case RESPONSE:
				return new MessageHeaderPair(
						msg.getResponse().getHeader(),
						msg.getResponse()
				);
			case RESPONSE_GET_CHANNELPIPELINES:
				return new MessageHeaderPair(
						msg.getGetChannelPipelinesResponse().getHeader(),
						msg.getGetChannelPipelinesResponse()
				);

			// EVENTS
			case EVENT_UPSTREAM_MESSAGE:
				return new MessageHeaderPair(
						msg.getUpstreamMessageEvent().getHeader(),
						msg.getUpstreamMessageEvent()
				);
			case EVENT_DEVICES_ATTACHED:
				return new MessageHeaderPair(
						msg.getDevicesAttachedEvent().getHeader(),
						msg.getDevicesAttachedEvent()
				);
			case EVENT_DEVICES_DETACHED:
				return new MessageHeaderPair(
						msg.getDevicesAttachedEvent().getHeader(),
						msg.getDevicesAttachedEvent()
				);
			case EVENT_GATEWAY_CONNECTED:
				return new MessageHeaderPair(
						msg.getGatewayConnectedEvent().getHeader(),
						msg.getGatewayConnectedEvent()
				);
			case EVENT_GATEWAY_DISCONNECTED:
				return new MessageHeaderPair(
						msg.getGatewayDisconnectedEvent().getHeader(),
						msg.getGatewayDisconnectedEvent()
				);
			case EVENT_NOTIFICATION:
				return new MessageHeaderPair(
						msg.getNotificationEvent().getHeader(),
						msg.getNotificationEvent()
				);
			case EVENT_RESERVATION_STARTED:
				return new MessageHeaderPair(
						msg.getReservationStartedEvent().getHeader(),
						msg.getReservationStartedEvent()
				);
			case EVENT_RESERVATION_ENDED:
				return new MessageHeaderPair(
						msg.getReservationEndedEvent().getHeader(),
						msg.getReservationEndedEvent()
				);
			case EVENT_RESERVATION_MADE:
				return new MessageHeaderPair(
						msg.getReservationMadeEvent().getHeader(),
						msg.getReservationMadeEvent()
				);
			case EVENT_RESERVATION_CANCELLED:
				return new MessageHeaderPair(
						msg.getReservationCancelledEvent().getHeader(),
						msg.getReservationCancelledEvent()
				);
			case EVENT_RESERVATION_OPENED:
				return new MessageHeaderPair(
						msg.getReservationOpenedEvent().getHeader(),
						msg.getReservationOpenedEvent()
				);
			case EVENT_RESERVATION_CLOSED:
				return new MessageHeaderPair(
						msg.getReservationClosedEvent().getHeader(),
						msg.getReservationClosedEvent()
				);
			case EVENT_RESERVATION_FINALIZED:
				return new MessageHeaderPair(
						msg.getReservationFinalizedEvent().getHeader(),
						msg.getReservationFinalizedEvent()
				);
			case EVENT_DEVICE_CONFIG_CREATED:
				return new MessageHeaderPair(
						msg.getDeviceConfigCreatedEvent().getHeader(),
						msg.getDeviceConfigCreatedEvent()
				);
			case EVENT_DEVICE_CONFIG_UPDATED:
				return new MessageHeaderPair(
						msg.getDeviceConfigUpdatedEvent().getHeader(),
						msg.getDeviceConfigUpdatedEvent()
				);
			case EVENT_DEVICE_CONFIG_DELETED:
				return new MessageHeaderPair(
						msg.getDeviceConfigDeletedEvent().getHeader(),
						msg.getDeviceConfigDeletedEvent()
				);
			case EVENT_ACK:
				return new MessageHeaderPair(
						msg.getEventAck().getHeader(),
						msg.getEventAck()
				);
			default:
				throw new IllegalArgumentException("Event message type \"" + msg.getType() + "\" unknown!");
		}
	}
}
