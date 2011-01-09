package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceImpl;
import de.uniluebeck.itm.tr.runtime.portalapp.WSNServiceHandle;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNNodeMessageReceiver;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class ProtobufControllerServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(ProtobufControllerServerHandler.class.getName());

	private boolean firstMessage = true;

	private final SessionManagementServiceImpl sessionManagement;

	private WSNServiceHandle wsnServiceHandle;

	private Channel channel;

	public ProtobufControllerServerHandler(final SessionManagementServiceImpl sessionManagement) {
		this.sessionManagement = sessionManagement;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		log.info("Client connected: {}", e);
		channel = e.getChannel();
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		log.info("Client disconnected: {}", e);
		if (wsnServiceHandle != null) {
			wsnServiceHandle.getWsnApp().removeNodeMessageReceiver(wsnNodeMessageReceiver);
		}
		super.channelDisconnected(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		WisebedProtocol.Envelope envelope = (WisebedProtocol.Envelope) e.getMessage();
		switch (envelope.getBodyType()) {
			case SECRET_RESERVATION_KEYS:

				if (firstMessage) {
					firstMessage = false;
				}

				checkArgument(!firstMessage, "Secret reservation keys are only allowed to be sent as the very first message.");
				checkArgument(envelope.hasSecretReservationKeys(), "Envelope is missing secret reservation keys");

				log.debug("ProtobufControllerServerHandler.receivedSecretReservationKeys()");

				wsnServiceHandle = sessionManagement.getWsnServiceHandle(
						envelope.getSecretReservationKeys().getKeys(0).getKey()
				);

				if (wsnServiceHandle == null) {
					log.debug("Received unknown secret reservation key. Closing channel to client.");
					channel.close();
				} else {
					log.debug("Received valid secret reservation key. Adding listener to WSN App instance.");
					wsnServiceHandle.getWsnApp().addNodeMessageReceiver(wsnNodeMessageReceiver);
				}

				break;
			default:
				log.warn("Received message other than secret reservation keys which is not allowed.");
				e.getChannel().close();
				break;
		}

	}

	private final WSNNodeMessageReceiver wsnNodeMessageReceiver = new WSNNodeMessageReceiver() {
		@Override
		public void receive(WSNAppMessages.Message message) {
			channel.write(convert(message));
		}
	};

	private WisebedProtocol.Envelope convert(WSNAppMessages.Message message) {

		WisebedProtocol.Message.Builder messageBuilder = WisebedProtocol.Message.newBuilder();
		messageBuilder.setTimestamp(message.getTimestamp());

		if (message.hasBinaryMessage()) {

			WisebedProtocol.Message.NodeBinary.Builder nodeBinaryBuilder = WisebedProtocol.Message.NodeBinary.newBuilder()
					.setSourceNodeUrn(message.getSourceNodeId())
					.setType(message.getBinaryMessage().getBinaryType())
					.setData(message.getBinaryMessage().getBinaryData());
			messageBuilder.setNodeBinary(nodeBinaryBuilder);
			messageBuilder.setType(WisebedProtocol.Message.Type.NODE_BINARY);

		} else if (message.hasTextMessage()) {

			WisebedProtocol.Message.NodeText.Builder nodeTextBuilder = WisebedProtocol.Message.NodeText.newBuilder()
					.setSourceNodeUrn(message.getSourceNodeId())
					.setLevel(WisebedProtocol.Message.Level.valueOf(message.getTextMessage().getMessageLevel().getNumber()))
					.setText(message.getTextMessage().getMsg());
			messageBuilder.setNodeText(nodeTextBuilder);
			messageBuilder.setType(WisebedProtocol.Message.Type.NODE_TEXT);

		}

		return WisebedProtocol.Envelope.newBuilder()
				.setBodyType(WisebedProtocol.Envelope.BodyType.MESSAGE)
				.setMessage(messageBuilder)
				.build();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		log.warn("Unexpected exception from downstream: {}", e);
		e.getChannel().close();
	}
}
