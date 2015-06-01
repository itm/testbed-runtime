package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerImpl;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.controller.SingleNodeRequestStatus;
import org.joda.time.DateTime;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.transform;

public class DeliveryManagerAdapter extends DeliveryManagerImpl {

	private final Reservation reservation;

	@Inject
	public DeliveryManagerAdapter(@Assisted final Reservation reservation) {
		this.reservation = reservation;
	}

	@Override
	protected void doStart() {
		reservation.getEventBus().register(this);
		super.doStart();
	}

	@Override
	protected void doStop() {
		reservation.getEventBus().unregister(this);
		super.doStop();
	}

	@Subscribe
	public void onUpstreamMessageEvent(final UpstreamMessageEvent event) {
		log.trace("DeliveryManagerAdapter.onUpstreamMessageEvent({})", event);
		receive(convert(event));
	}

	@Subscribe
	public void onDevicesAttachedEvent(final DevicesAttachedEvent event) {
		log.trace("DeliveryManagerAdapter.onDevicesAttachedEvent({})", event);
		nodesAttached(new DateTime(event.getHeader().getTimestamp()), transform(event.getHeader().getNodeUrnsList(), NodeUrn::new));
	}

	@Subscribe
	public void onDevicesDetachedEvent(final DevicesDetachedEvent event) {
		log.trace("DeliveryManagerAdapter.onDevicesDetachedEvent({})", event);
		nodesDetached(new DateTime(event.getHeader().getTimestamp()), transform(event.getHeader().getNodeUrnsList(), NodeUrn::new));
	}

	@Subscribe
	public void onNotification(final NotificationEvent event) {
		log.trace("DeliveryManagerAdapter.onNotification({})", event);
		receiveNotification(convert(event));
	}

	@Subscribe
	public void onSingleNodeResponse(final Response response) {
		log.trace("DeliveryManagerAdapter.onSingleNodeResponse({})", response);
		convert(response);
		receiveStatus();
	}

	@Subscribe
	public void onSingleNodeProgress(final Progress progress) {
		log.trace("DeliveryManagerAdapter.onSingleNodeProgress({})", progress);
		receiveStatus(convert(progress));
	}

	@Subscribe
	public void onReservationStartedEvent(final ReservationStartedEvent event) {
		log.trace("DeliveryManagerAdapter.onReservationStartedEvent({})", event);
		if (!event.getHeader().getSerializedReservationKey().equals(reservation.getSerializedKey())) {
			throw new RuntimeException("This should not be possible!");
		}
		reservationStarted(reservation.getInterval().getStart());
	}

	@Subscribe
	public void onReservationEndedEvent(final ReservationEndedEvent event) {
		log.trace("DeliveryManagerAdapter.onReservationEndedEvent({})", event);
		if (!event.getHeader().getSerializedReservationKey().equals(reservation.getSerializedKey())) {
			throw new RuntimeException("This should not be possible!");
		}
		reservationEnded(reservation.getInterval().getEnd());
	}

	private List<RequestStatus> convert(final Progress progress) {

		return progress.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).map(n -> {

			final SingleNodeRequestStatus status = new SingleNodeRequestStatus();
			status.setNodeUrn(n);
			status.setValue(progress.getProgressInPercent());
			status.setCompleted(false);
			status.setSuccess(false);

			final RequestStatus requestStatus = new RequestStatus();
			requestStatus.setRequestId(progress.getHeader().getCorrelationId());
			requestStatus.getSingleNodeRequestStatus().add(status);

			return requestStatus;

		}).collect(Collectors.toList());
	}

	private List<RequestStatus> convert(final Response response) {

		return response.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).map(n -> {

			final SingleNodeRequestStatus status = new SingleNodeRequestStatus();
			status.setNodeUrn(n);
			status.setValue(response.getStatusCode());
			status.setCompleted(true);
			status.setSuccess(!response.hasErrorMessage());
			if (response.hasErrorMessage()) {
				status.setMsg(response.getErrorMessage());
			}

			final RequestStatus requestStatus = new RequestStatus();
			requestStatus.setRequestId(response.getHeader().getCorrelationId());
			requestStatus.getSingleNodeRequestStatus().add(status);

			return requestStatus;

		}).collect(Collectors.toList());
	}

	private List<Notification> convert(final NotificationEvent event) {

		return event.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).map(n -> {

			final Notification notification = new Notification();
			notification.setNodeUrn(n);
			notification.setTimestamp(new DateTime(event.getHeader().getTimestamp()));
			notification.setMsg(event.getMessage());

			return notification;

		}).collect(Collectors.toList());
	}

	private List<Message> convert(final UpstreamMessageEvent event) {

		return event.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).map(n -> {

			final Message msg = new Message();
			msg.setSourceNodeUrn(n);
			msg.setTimestamp(new DateTime(event.getHeader().getTimestamp()));
			msg.setBinaryData(event.getMessageBytes().toByteArray());

			return msg;

		}).collect(Collectors.toList());
	}
}
