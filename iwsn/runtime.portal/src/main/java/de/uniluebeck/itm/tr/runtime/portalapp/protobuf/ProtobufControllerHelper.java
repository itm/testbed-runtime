package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import com.google.inject.internal.Nullable;
import com.google.protobuf.ByteString;
import eu.wisebed.testbed.api.wsn.ControllerHelper;
import eu.wisebed.testbed.api.wsn.v211.Message;
import eu.wisebed.testbed.api.wsn.v211.RequestStatus;
import eu.wisebed.testbed.api.wsn.v211.Status;
import eu.wisebed.testbed.api.wsn.v211.UnknownNodeUrnException_Exception;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;


public class ProtobufControllerHelper extends ControllerHelper {

	private final ChannelGroup channels = new DefaultChannelGroup();

	public ProtobufControllerHelper(@Nullable Integer maximumDeliveryQueueSize) {
		super(maximumDeliveryQueueSize);
	}

	@Override
	public int getControllerCount() {
		return super.getControllerCount() + channels.size();
	}

	@Override
	public void receive(Message message) {
		if (channels.size() > 0) {
			channels.write(convert(message));
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
	public void receiveUnkownNodeUrnRequestStatus(UnknownNodeUrnException_Exception e, String requestId) {
		if (channels.size() > 0) {

			WisebedProtocol.RequestStatus.Builder requestStatusBuilder = WisebedProtocol.RequestStatus.newBuilder()
					.setRequestId(requestId);

			for (String urn : e.getFaultInfo().getUrn()) {
				WisebedProtocol.RequestStatus.Status.Builder statusBuilder = WisebedProtocol.RequestStatus.Status.newBuilder()
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
		super.receiveUnkownNodeUrnRequestStatus(e, requestId);
	}

	public void addChannel(Channel channel) {
		channels.add(channel);
	}

	public void removeChannel(Channel channel) {
		channels.remove(channel);
	}

	private WisebedProtocol.Envelope convert(Message message) {

		WisebedProtocol.Message.Builder messageBuilder = WisebedProtocol.Message.newBuilder();
		messageBuilder.setTimestamp(message.getTimestamp().toXMLFormat());

		if (message.getBinaryMessage() != null) {

			WisebedProtocol.Message.NodeBinary.Builder nodeBinaryBuilder = WisebedProtocol.Message.NodeBinary.newBuilder()
					.setSourceNodeUrn(message.getSourceNodeId())
					.setType(message.getBinaryMessage().getBinaryType())
					.setData(ByteString.copyFrom(message.getBinaryMessage().getBinaryData()));
			messageBuilder.setNodeBinary(nodeBinaryBuilder);
			messageBuilder.setType(WisebedProtocol.Message.Type.NODE_BINARY);

		} else if (message.getTextMessage() != null) {

			WisebedProtocol.Message.NodeText.Builder nodeTextBuilder = WisebedProtocol.Message.NodeText.newBuilder()
					.setSourceNodeUrn(message.getSourceNodeId())
					.setLevel(WisebedProtocol.Message.Level.valueOf(message.getTextMessage().getMessageLevel().value()))
					.setText(message.getTextMessage().getMsg());

			messageBuilder.setNodeText(nodeTextBuilder);
			messageBuilder.setType(WisebedProtocol.Message.Type.NODE_TEXT);

		}

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
