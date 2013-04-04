package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerImpl;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.controller.Status;
import org.joda.time.DateTime;

import static com.google.common.collect.Iterables.transform;
import static de.uniluebeck.itm.tr.iwsn.common.NodeUrnHelper.STRING_TO_NODE_URN;

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
		nodesAttached(new DateTime(event.getTimestamp()), transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
	}

	@Subscribe
	public void onDevicesDetachedEvent(final DevicesDetachedEvent event) {
		log.trace("DeliveryManagerAdapter.onDevicesDetachedEvent({})", event);
		nodesDetached(new DateTime(event.getTimestamp()), transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
	}

	@Subscribe
	public void onNotification(final NotificationEvent event) {
		log.trace("DeliveryManagerAdapter.onNotification({})", event);
		receiveNotification(convert(event));
	}

	@Subscribe
	public void onSingleNodeResponse(final SingleNodeResponse response) {
		log.trace("DeliveryManagerAdapter.onSingleNodeResponse({})", response);
		receiveStatus(convert(response));
	}

	@Subscribe
	public void onSingleNodeProgress(final SingleNodeProgress progress) {
		log.trace("DeliveryManagerAdapter.onSingleNodeProgress({})", progress);
		receiveStatus(convert(progress));
	}

	@Subscribe
	public void onReservationStartedEvent(final ReservationStartedEvent event) {
		log.trace("DeliveryManagerAdapter.onReservationStartedEvent({})", event);
		reservationStarted(event.getReservation().getInterval().getStart());
	}

	@Subscribe
	public void onReservationEndedEvent(final ReservationEndedEvent event) {
		log.trace("DeliveryManagerAdapter.onReservationEndedEvent({})", event);
		reservationEnded(event.getReservation().getInterval().getEnd());
	}

	private RequestStatus convert(final SingleNodeProgress progress) {

		final Status status = new Status();
		status.setNodeUrn(new NodeUrn(progress.getNodeUrn()));
		status.setValue(progress.getProgressInPercent());

		final RequestStatus requestStatus = new RequestStatus();
		requestStatus.setRequestId(progress.getRequestId());
		requestStatus.getStatus().add(status);

		return requestStatus;
	}

	private RequestStatus convert(final SingleNodeResponse response) {

		final Status status = new Status();
		status.setNodeUrn(new NodeUrn(response.getNodeUrn()));
		status.setValue(response.getStatusCode());
		if (response.hasErrorMessage()) {
			status.setMsg(response.getErrorMessage());
		}

		final RequestStatus requestStatus = new RequestStatus();
		requestStatus.setRequestId(response.getRequestId());
		requestStatus.getStatus().add(status);

		return requestStatus;
	}

	private Notification convert(final NotificationEvent event) {

		final Notification notification = new Notification();
		notification.setNodeUrn(new NodeUrn(event.getNodeUrn()));
		notification.setTimestamp(new DateTime(event.getTimestamp()));
		notification.setMsg(event.getMessage());

		return notification;
	}

	private Message convert(final UpstreamMessageEvent event) {

		final Message msg = new Message();
		msg.setSourceNodeUrn(new NodeUrn(event.getSourceNodeUrn()));
		msg.setTimestamp(new DateTime(event.getTimestamp()));
		msg.setBinaryData(event.getMessageBytes().toByteArray());

		return msg;
	}
}
