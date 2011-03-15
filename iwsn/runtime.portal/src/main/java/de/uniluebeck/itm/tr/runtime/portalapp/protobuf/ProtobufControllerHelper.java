package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import com.google.protobuf.ByteString;
import eu.wisebed.testbed.api.wsn.ControllerHelper;
import eu.wisebed.testbed.api.wsn.v22.Message;
import eu.wisebed.testbed.api.wsn.v22.RequestStatus;
import eu.wisebed.testbed.api.wsn.v22.Status;
import eu.wisebed.testbed.api.wsn.v22.UnknownNodeUrnException_Exception;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProtobufControllerHelper extends ControllerHelper {

	private static final Logger log = LoggerFactory.getLogger(ProtobufControllerHelper.class);

	private final ChannelGroup channels = new DefaultChannelGroup();

	private volatile int currentMessageDeliveryQueueSize = 0;

	private volatile long lastBackendMessage = System.currentTimeMillis();

	private ChannelGroupFutureListener messageDeliveryListener = new ChannelGroupFutureListener() {
		@Override
		public void operationComplete(final ChannelGroupFuture future) throws Exception {
			currentMessageDeliveryQueueSize--;
		}
	};

	public ProtobufControllerHelper(Integer maximumDeliveryQueueSize) {
		super(maximumDeliveryQueueSize);
	}

	@Override
	public int getControllerCount() {
		return super.getControllerCount() + channels.size();
	}

	@Override
	public void receive(Message message) {
		if (channels.size() > 0) {

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

				WisebedProtocol.Message.Backend.Builder backendBuilder = WisebedProtocol.Message.Backend.newBuilder()
						.setText("Your experiment is generating too many messages to be delivered. "
								+ "Therefore the backend drops messages. "
								+ "Please make sure the message rate is lowered."
						);

				WisebedProtocol.Message backendMessage = WisebedProtocol.Message.newBuilder()
						.setType(WisebedProtocol.Message.Type.BACKEND)
						.setBackend(backendBuilder)
						.build();

				channels.write(backendMessage).awaitUninterruptibly();
				lastBackendMessage = System.currentTimeMillis();

			}
		}
		super.receive(message);
	}

	@Override
	public void receiveStatus(RequestStatus requestStatus) {
		if (channels.size() > 0) {
			channels.write(convert(requestStatus));
		}
		super.receiveStatus(requestStatus);
	}

	@Override
	public void receiveUnknownNodeUrnRequestStatus(UnknownNodeUrnException_Exception e, String requestId) {
		if (channels.size() > 0) {

			WisebedProtocol.RequestStatus.Builder requestStatusBuilder = WisebedProtocol.RequestStatus.newBuilder()
					.setRequestId(requestId);

			for (String urn : e.getFaultInfo().getUrn()) {
				WisebedProtocol.RequestStatus.Status.Builder statusBuilder =
						WisebedProtocol.RequestStatus.Status.newBuilder()
								.setNodeUrn(urn)
								.setMessage(e.getFaultInfo().getMessage())
								.setValue(-1);
				requestStatusBuilder.addStatus(statusBuilder);
			}

			WisebedProtocol.Envelope envelope = WisebedProtocol.Envelope.newBuilder()
					.setBodyType(WisebedProtocol.Envelope.BodyType.REQUEST_STATUS)
					.setRequestStatus(requestStatusBuilder)
					.build();

			channels.write(envelope);
		}
		super.receiveUnknownNodeUrnRequestStatus(e, requestId);
	}

	public void addChannel(Channel channel) {
		channels.add(channel);
	}

	public void removeChannel(Channel channel) {
		channels.remove(channel);
	}

	private WisebedProtocol.Envelope convert(Message message) {

		WisebedProtocol.Message.Builder messageBuilder = WisebedProtocol.Message.newBuilder()
				.setTimestamp(message.getTimestamp().toXMLFormat());

		WisebedProtocol.Message.NodeBinary.Builder nodeBinaryBuilder = WisebedProtocol.Message.NodeBinary.newBuilder()
				.setSourceNodeUrn(message.getSourceNodeId())
				.setData(ByteString.copyFrom(message.getBinaryData()));

		messageBuilder.setNodeBinary(nodeBinaryBuilder);
		messageBuilder.setType(WisebedProtocol.Message.Type.NODE_BINARY);

		return WisebedProtocol.Envelope.newBuilder()
				.setBodyType(WisebedProtocol.Envelope.BodyType.MESSAGE)
				.setMessage(messageBuilder)
				.build();
	}

	private WisebedProtocol.Envelope convert(RequestStatus requestStatus) {

		WisebedProtocol.RequestStatus.Builder requestStatusBuilder = WisebedProtocol.RequestStatus.newBuilder()
				.setRequestId(requestStatus.getRequestId());

		for (Status status : requestStatus.getStatus()) {
			requestStatusBuilder.addStatus(WisebedProtocol.RequestStatus.Status.newBuilder()
					.setValue(status.getValue())
					.setMessage(status.getMsg())
					.setNodeUrn(status.getNodeId())
			);
		}

		return WisebedProtocol.Envelope.newBuilder()
				.setBodyType(WisebedProtocol.Envelope.BodyType.REQUEST_STATUS)
				.setRequestStatus(requestStatusBuilder)
				.build();
	}
}
