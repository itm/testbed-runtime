package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerImpl;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesAttachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent;
import de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
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
		reservation.getReservationEventBus().register(this);
		super.doStart();
	}

	@Override
	protected void doStop() {
		reservation.getReservationEventBus().unregister(this);
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
		nodesAttached(transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
	}

	@Subscribe
	public void onDevicesDetachedEvent(final DevicesDetachedEvent event) {
		log.trace("DeliveryManagerAdapter.onDevicesDetachedEvent({})", event);
		nodesDetached(transform(event.getNodeUrnsList(), STRING_TO_NODE_URN));
	}

	@Subscribe
	public void onNotification(final NotificationEvent event) {
		log.trace("DeliveryManagerAdapter.onNotification({})", event);
		receiveNotification(convert(event));
	}

	private Notification convert(final NotificationEvent event) {
		Notification notification = new Notification();
		notification.setNodeUrn(new NodeUrn(event.getNodeUrn()));
		notification.setTimestamp(new DateTime(event.getTimestamp()));
		notification.setMsg(event.getMessage());
		return notification;
	}

	private Message convert(final UpstreamMessageEvent event) {
		Message msg = new Message();
		msg.setSourceNodeUrn(new NodeUrn(event.getSourceNodeUrn()));
		msg.setTimestamp(new DateTime(event.getTimestamp()));
		msg.setBinaryData(event.getMessageBytes().toByteArray());
		return msg;
	}
}
