package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;

public class EventMessageEvent {

	public static final Set<Class> MESSAGE_CLASS_TYPES = new HashSet<>();

	public static final Set<MessageType> MESSAGE_TYPES = new HashSet<>();

	static {

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

		MESSAGE_TYPES.add(MessageType.EVENT_ACK);
		MESSAGE_TYPES.add(MessageType.EVENT_DEVICE_CONFIG_CREATED);
		MESSAGE_TYPES.add(MessageType.EVENT_DEVICE_CONFIG_DELETED);
		MESSAGE_TYPES.add(MessageType.EVENT_DEVICE_CONFIG_UPDATED);
		MESSAGE_TYPES.add(MessageType.EVENT_DEVICES_ATTACHED);
		MESSAGE_TYPES.add(MessageType.EVENT_DEVICES_DETACHED);
		MESSAGE_TYPES.add(MessageType.EVENT_GATEWAY_CONNECTED);
		MESSAGE_TYPES.add(MessageType.EVENT_GATEWAY_DISCONNECTED);
		MESSAGE_TYPES.add(MessageType.EVENT_NOTIFICATION);
		MESSAGE_TYPES.add(MessageType.EVENT_RESERVATION_CANCELLED);
		MESSAGE_TYPES.add(MessageType.EVENT_RESERVATION_CLOSED);
		MESSAGE_TYPES.add(MessageType.EVENT_RESERVATION_ENDED);
		MESSAGE_TYPES.add(MessageType.EVENT_RESERVATION_FINALIZED);
		MESSAGE_TYPES.add(MessageType.EVENT_RESERVATION_MADE);
		MESSAGE_TYPES.add(MessageType.EVENT_RESERVATION_OPENED);
		MESSAGE_TYPES.add(MessageType.EVENT_RESERVATION_STARTED);
		MESSAGE_TYPES.add(MessageType.EVENT_UPSTREAM_MESSAGE);
	}

	public final EventHeader header;

	public final MessageLite message;

	public EventMessageEvent(EventHeader header, MessageLite unwrappedMessage) {
		this.header = header;
		this.message = unwrappedMessage;
	}

	public static boolean isUnwrappedEventMessageEvent(Object obj) {
		return MESSAGE_CLASS_TYPES.contains(obj.getClass());
	}

	public static boolean isWrappedEventMessageEvent(Message msg) {
		return MESSAGE_TYPES.contains(msg.getType());
	}

	public static EventMessageEvent fromUnwrapped(Object obj) {

		if (!isUnwrappedEventMessageEvent(obj)) {
			throw new IllegalArgumentException("Unknown message type \"" + obj.getClass() + "\"!");
		}

		try {
			return new EventMessageEvent(
					(EventHeader) obj.getClass().getMethod("getHeader").invoke(obj),
					(MessageLite) obj
			);
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	public static EventMessageEvent fromWrapped(Message msg) {

		switch (msg.getType()) {
			case EVENT_UPSTREAM_MESSAGE:
				return new EventMessageEvent(
						msg.getUpstreamMessageEvent().getHeader(),
						msg.getUpstreamMessageEvent()
				);
			case EVENT_DEVICES_ATTACHED:
				return new EventMessageEvent(
						msg.getDevicesAttachedEvent().getHeader(),
						msg.getDevicesAttachedEvent()
				);
			case EVENT_DEVICES_DETACHED:
				return new EventMessageEvent(
						msg.getDevicesAttachedEvent().getHeader(),
						msg.getDevicesAttachedEvent()
				);
			case EVENT_GATEWAY_CONNECTED:
				return new EventMessageEvent(
						msg.getGatewayConnectedEvent().getHeader(),
						msg.getGatewayConnectedEvent()
				);
			case EVENT_GATEWAY_DISCONNECTED:
				return new EventMessageEvent(
						msg.getGatewayDisconnectedEvent().getHeader(),
						msg.getGatewayDisconnectedEvent()
				);
			case EVENT_NOTIFICATION:
				return new EventMessageEvent(
						msg.getNotificationEvent().getHeader(),
						msg.getNotificationEvent()
				);
			case EVENT_RESERVATION_STARTED:
				return new EventMessageEvent(
						msg.getReservationStartedEvent().getHeader(),
						msg.getReservationStartedEvent()
				);
			case EVENT_RESERVATION_ENDED:
				return new EventMessageEvent(
						msg.getReservationEndedEvent().getHeader(),
						msg.getReservationEndedEvent()
				);
			case EVENT_RESERVATION_MADE:
				return new EventMessageEvent(
						msg.getReservationMadeEvent().getHeader(),
						msg.getReservationMadeEvent()
				);
			case EVENT_RESERVATION_CANCELLED:
				return new EventMessageEvent(
						msg.getReservationCancelledEvent().getHeader(),
						msg.getReservationCancelledEvent()
				);
			case EVENT_RESERVATION_OPENED:
				return new EventMessageEvent(
						msg.getReservationOpenedEvent().getHeader(),
						msg.getReservationOpenedEvent()
				);
			case EVENT_RESERVATION_CLOSED:
				return new EventMessageEvent(
						msg.getReservationClosedEvent().getHeader(),
						msg.getReservationClosedEvent()
				);
			case EVENT_RESERVATION_FINALIZED:
				return new EventMessageEvent(
						msg.getReservationFinalizedEvent().getHeader(),
						msg.getReservationFinalizedEvent()
				);
			case EVENT_DEVICE_CONFIG_CREATED:
				return new EventMessageEvent(
						msg.getDeviceConfigCreatedEvent().getHeader(),
						msg.getDeviceConfigCreatedEvent()
				);
			case EVENT_DEVICE_CONFIG_UPDATED:
				return new EventMessageEvent(
						msg.getDeviceConfigUpdatedEvent().getHeader(),
						msg.getDeviceConfigUpdatedEvent()
				);
			case EVENT_DEVICE_CONFIG_DELETED:
				return new EventMessageEvent(
						msg.getDeviceConfigDeletedEvent().getHeader(),
						msg.getDeviceConfigDeletedEvent()
				);
			case EVENT_ACK:
				return new EventMessageEvent(
						msg.getEventAck().getHeader(),
						msg.getEventAck()
				);
			default:
				throw new IllegalArgumentException("Event message type \"" + msg.getType() + "\" unknown!");
		}
	}
}
