package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.controller.Notification;
import eu.wisebed.api.v3.controller.RequestStatus;
import eu.wisebed.api.v3.controller.Status;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;


public class ProtobufDeliveryManager extends DeliveryManager {

	private static final Logger log = LoggerFactory.getLogger(ProtobufDeliveryManager.class);

	private final ChannelGroup channels = new DefaultChannelGroup();

	private volatile int currentMessageDeliveryQueueSize = 0;

	private volatile long lastBackendMessage = System.currentTimeMillis();

	private ChannelGroupFutureListener messageDeliveryListener = new ChannelGroupFutureListener() {
		@Override
		public void operationComplete(final ChannelGroupFuture future) throws Exception {
			currentMessageDeliveryQueueSize--;
		}
	};

	public ProtobufDeliveryManager(final @Nullable Integer maximumDeliveryQueueSize) {
		super(maximumDeliveryQueueSize);
	}

	@Override
	public void reservationEnded() {
		if (channels.size() > 0) {
			channels.close();
		}
		super.reservationEnded();
	}

	@Override
	public void receive(final List<Message> messages) {

		if (channels.size() > 0) {

			for (final Message message : messages) {

				final long BACKEND_MESSAGE_INTERVAL = 1000;

				// only send message to client if delivery queue is smaller than maximum
				if (currentMessageDeliveryQueueSize < maximumDeliveryQueueSize) {

					// count the messages that still have to be delivered
					currentMessageDeliveryQueueSize++;

					// write message to clients
					// messageDeliveryListener will decrease delivery queue size counter
					channels.write(convert(message)).addListener(messageDeliveryListener);
				}

				// inform the user of dropped messages every BACKEND_MESSAGE_INTERVAL milliseconds
				else if (System.currentTimeMillis() - lastBackendMessage > BACKEND_MESSAGE_INTERVAL) {

					log.warn("Dropped one or more messages. Informing protobuf controllers.");

					WisebedMessages.Notification.Builder notification = WisebedMessages.Notification.newBuilder()
							.setTimestamp(new DateTime().toString())
							.setMsg("Your experiment is generating too many messages to be delivered. "
									+ "Therefore the backend drops messages. "
									+ "Please make sure the message rate is lowered."
							);

					WisebedMessages.Envelope envelope = WisebedMessages.Envelope.newBuilder()
							.setMessageType(WisebedMessages.MessageType.NOTIFICATION)
							.setNotification(notification)
							.build();

					channels.write(envelope);
					lastBackendMessage = System.currentTimeMillis();
				}
			}
		}

		super.receive(messages);

	}

	@Override
	public void receive(final Message... messages) {
		receive(Lists.newArrayList(messages));
	}

	@Override
	public void receiveFailureStatusMessages(final List<NodeUrn> nodeUrns, final long requestId, final Exception e,
											 final int statusValue) {

		if (channels.size() > 0) {

			RequestStatus requestStatus = new RequestStatus();
			requestStatus.setRequestId(requestId);

			for (NodeUrn nodeId : nodeUrns) {
				Status status = new Status();
				status.setNodeUrn(nodeId);
				status.setValue(statusValue);
				status.setMsg(e.getMessage());
				requestStatus.getStatus().add(status);
			}

			channels.write(convert(requestStatus));

		}

		super.receiveFailureStatusMessages(nodeUrns, requestId, e, statusValue);
	}

	@Override
	public void receiveNotification(final List<Notification> notifications) {

		if (channels.size() > 0) {
			for (Notification notification : notifications) {
				channels.write(convert(notification));
			}
		}

		super.receiveNotification(notifications);
	}

	@Override
	public void receiveNotification(final Notification... notifications) {
		receiveNotification(Lists.newArrayList(notifications));
	}

	@Override
	public void receiveStatus(final List<RequestStatus> statuses) {

		if (channels.size() > 0) {
			for (RequestStatus status : statuses) {
				channels.write(convert(status));
			}
		}

		super.receiveStatus(statuses);
	}

	@Override
	public void receiveStatus(final RequestStatus... statuses) {
		receiveStatus(Lists.newArrayList(statuses));
	}

	@Override
	public void receiveUnknownNodeUrnRequestStatus(final Set<NodeUrn> nodeUrns, final String msg,
												   final long requestId) {
		if (channels.size() > 0) {

			WisebedMessages.RequestStatus.Builder requestStatusBuilder = WisebedMessages.RequestStatus.newBuilder()
					.setRequestId(requestId);

			for (NodeUrn nodeUrn : nodeUrns) {
				WisebedMessages.RequestStatus.Status.Builder statusBuilder =
						WisebedMessages.RequestStatus.Status.newBuilder()
								.setNodeUrn(nodeUrn.toString())
								.setMessage(msg)
								.setValue(-1);
				requestStatusBuilder.addStatus(statusBuilder);
			}

			WisebedMessages.Envelope envelope = WisebedMessages.Envelope.newBuilder()
					.setMessageType(WisebedMessages.MessageType.REQUEST_STATUS)
					.setRequestStatus(requestStatusBuilder)
					.build();

			channels.write(envelope);
		}
		super.receiveUnknownNodeUrnRequestStatus(nodeUrns, msg, requestId);
	}

	public void addChannel(Channel channel) {
		channels.add(channel);
	}

	public void removeChannel(Channel channel) {
		channels.remove(channel);
	}

	private WisebedMessages.Envelope convert(final Notification notification) {

		WisebedMessages.Notification.Builder notificationBuilder = WisebedMessages.Notification.newBuilder()
				.setTimestamp(new DateTime(notification.getTimestamp().toGregorianCalendar()).toString())
				.setMsg(notification.getMsg());

		if (notification.getNodeUrn() != null) {
			notificationBuilder.setNodeUrn(notification.getNodeUrn().toString());
		}

		return WisebedMessages.Envelope.newBuilder()
				.setMessageType(WisebedMessages.MessageType.NOTIFICATION)
				.setNotification(notificationBuilder)
				.build();
	}

	private WisebedMessages.Envelope convert(Message message) {

		WisebedMessages.UpstreamMessage.Builder upstreamMessageBuilder = WisebedMessages.UpstreamMessage.newBuilder()
				.setMessageBytes(ByteString.copyFrom(message.getBinaryData()))
				.setSourceNodeUrn(message.getSourceNodeUrn().toString())
				.setTimestamp(message.getTimestamp().toString());

		return WisebedMessages.Envelope.newBuilder()
				.setMessageType(WisebedMessages.MessageType.UPSTREAM_MESSAGE)
				.setUpstreamMessage(upstreamMessageBuilder)
				.build();
	}

	private WisebedMessages.Envelope convert(RequestStatus requestStatus) {

		WisebedMessages.RequestStatus.Builder requestStatusBuilder = WisebedMessages.RequestStatus.newBuilder()
				.setRequestId(requestStatus.getRequestId());

		for (Status status : requestStatus.getStatus()) {
			requestStatusBuilder.addStatus(WisebedMessages.RequestStatus.Status.newBuilder()
					.setValue(status.getValue())
					.setMessage(status.getMsg())
					.setNodeUrn(status.getNodeUrn().toString())
			);
		}

		return WisebedMessages.Envelope.newBuilder()
				.setMessageType(WisebedMessages.MessageType.REQUEST_STATUS)
				.setRequestStatus(requestStatusBuilder)
				.build();
	}
}
