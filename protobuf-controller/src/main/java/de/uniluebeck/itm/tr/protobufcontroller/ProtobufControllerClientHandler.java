package de.uniluebeck.itm.tr.protobufcontroller;

import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class ProtobufControllerClientHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(ProtobufControllerClientHandler.class.getName());

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			logger.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		WisebedProtocol.Envelope envelope = (WisebedProtocol.Envelope) e.getMessage();
		switch (envelope.getBodyType()) {
			case MESSAGE:
				checkArgument(envelope.hasMessage(), "Envelope is missing message.");
				receivedMessage(envelope.getMessage());
				break;
			case REQUEST_STATUS:
				checkArgument(envelope.hasRequestStatus(), "Envelope is missing request status.");
				receivedRequestStatus(envelope.getRequestStatus());
				break;
			default:
				checkArgument(false, "Received message other than message or request status which is not allowed.");
				break;
		}

	}

	private void receivedRequestStatus(WisebedProtocol.RequestStatus requestStatus) {
		System.out.println(requestStatus);
	}

	private void receivedMessage(WisebedProtocol.Message message) {
		System.out.println(message);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.warn("Unexpected exception from downstream: {}", e);
		e.getChannel().close();
	}

}
