package de.uniluebeck.itm.wisebed.cmdlineclient.protobuf;

import static com.google.common.base.Preconditions.checkArgument;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class ProtobufControllerClientHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(ProtobufControllerClientHandler.class.getName());

	private ProtobufControllerClient protobufControllerClient;

	public ProtobufControllerClientHandler(ProtobufControllerClient protobufControllerClient) {
		this.protobufControllerClient = protobufControllerClient;
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			logger.debug(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		WisebedProtocol.Envelope envelope = (WisebedProtocol.Envelope) e.getMessage();
		switch (envelope.getBodyType()) {
			case MESSAGE:
				Preconditions.checkArgument(envelope.hasMessage(), "Envelope is missing message.");
				protobufControllerClient.receivedMessage(envelope.getMessage());
				break;
			case REQUEST_STATUS:
				Preconditions.checkArgument(envelope.hasRequestStatus(), "Envelope is missing request status.");
				protobufControllerClient.receivedRequestStatus(envelope.getRequestStatus());
				break;
			default:
				checkArgument(false, "Received message other than message or request status which is not allowed.");
				break;
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.warn("Unexpected exception from downstream: {}", e);
		e.getChannel().close();
	}

}
