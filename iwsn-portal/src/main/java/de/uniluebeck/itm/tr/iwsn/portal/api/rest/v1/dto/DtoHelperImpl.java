package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class DtoHelperImpl implements DtoHelper {

	public final Map<Class<? extends MessageLite>, Function<MessageHeaderPair, Iterable<? extends Object>>> encoders = new HashMap<>();

	@Inject
	public DtoHelperImpl(final ReservationManager reservationManager,
						 @Assisted final String secretReservationKeysBase64) {

		// REQUESTS
		encoders.put(AreNodesAliveRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(AreNodesConnectedRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(DisableNodesRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(DisableVirtualLinksRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(DisablePhysicalLinksRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(EnableNodesRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(EnablePhysicalLinksRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(EnableVirtualLinksRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(FlashImagesRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(GetChannelPipelinesRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(ResetNodesRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(SendDownstreamMessagesRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(SetChannelPipelinesRequest.class, RequestMessage.CONVERT.andThen(SingleItemIterable::new));

		// RESPONSES
		encoders.put(Progress.class, SingleNodeProgressMessage::convert);
		encoders.put(Response.class, SingleNodeResponseMessage::convert);
		encoders.put(GetChannelPipelinesResponse.class, GetChannelPipelinesResponseMessage.CONVERT.andThen(SingleItemIterable::new));

		// EVENTS
		encoders.put(UpstreamMessageEvent.class, WebSocketUpstreamMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(DevicesAttachedEvent.class, DevicesAttachedMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(DevicesDetachedEvent.class, DevicesDetachedMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(GatewayConnectedEvent.class, GatewayConnectedMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(GatewayDisconnectedEvent.class, GatewayDisconnectedMessage.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(NotificationEvent.class, WebSocketNotificationMessage.CONVERT.andThen(SingleItemIterable::new));

		Function<MessageHeaderPair, Reservation> getReservation = (pair) ->
				reservationManager.getReservation(secretReservationKeysBase64);

		encoders.put(ReservationStartedEvent.class, getReservation.andThen(ReservationStartedMessage::new).andThen(SingleItemIterable::new));
		encoders.put(ReservationEndedEvent.class, getReservation.andThen(ReservationEndedMessage::new).andThen(SingleItemIterable::new));
		encoders.put(ReservationMadeEvent.class, getReservation.andThen(ReservationMadeMessage::new).andThen(SingleItemIterable::new));
		encoders.put(ReservationCancelledEvent.class, getReservation.andThen(ReservationCancelledMessage::new).andThen(SingleItemIterable::new));
		encoders.put(ReservationOpenedEvent.class, getReservation.andThen(ReservationOpenedMessage::new).andThen(SingleItemIterable::new));
		encoders.put(ReservationClosedEvent.class, getReservation.andThen(ReservationCancelledMessage::new).andThen(SingleItemIterable::new));
		encoders.put(ReservationFinalizedEvent.class, getReservation.andThen(ReservationFinalizedMessage::new).andThen(SingleItemIterable::new));

		encoders.put(DeviceConfigCreatedEvent.class, DeviceConfigEvent.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(DeviceConfigUpdatedEvent.class, DeviceConfigEvent.CONVERT.andThen(SingleItemIterable::new));
		encoders.put(DeviceConfigDeletedEvent.class, DeviceConfigEvent.CONVERT.andThen(SingleItemIterable::new));

		encoders.put(EventAck.class, pair -> {
			throw new RuntimeException("Didn't expect EventAck to show up here");
		});
	}

	@Override
	public Iterable<Object> encodeToJsonPojo(MessageHeaderPair pair) {
		//noinspection unchecked
		return (Iterable<Object>) encoders.get(pair.message.getClass()).apply(pair);
	}

	private static class SingleItemIterable implements Iterable<Object> {

		private final Object item;

		public SingleItemIterable(Object item) {
			this.item = item;
		}

		@Override
		public Iterator<Object> iterator() {
			return new Iterator<Object>() {

				private volatile boolean first = true;
				private volatile boolean returnedFirst = false;

				@Override
				public boolean hasNext() {
					if (first) {
						first = false;
						return true;
					}
					return false;
				}

				@Override
				public Object next() {
					if (!returnedFirst) {
						return item;
					}
					throw new NoSuchElementException();
				}
			};
		}
	}
}
