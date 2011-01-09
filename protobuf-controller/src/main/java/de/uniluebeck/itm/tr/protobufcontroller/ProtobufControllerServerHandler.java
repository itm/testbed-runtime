package de.uniluebeck.itm.tr.protobufcontroller;

import com.google.common.collect.Maps;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class ProtobufControllerServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(ProtobufControllerServerHandler.class.getName());

	boolean firstMessage = true;

	boolean authenticated = false;

	private final ScheduledExecutorService executor;

	private Map<Channel, ScheduledFuture<?>> schedules = Maps.newHashMap();

	public ProtobufControllerServerHandler(final ScheduledExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			log.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		schedules.remove(e.getChannel()).cancel(true);
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

				receivedSecretReservationKeys(e.getChannel(), envelope.getSecretReservationKeys());

				break;
			default:
				log.warn("Received message other than secret reservation keys which is not allowed.");
				e.getChannel().close();
				break;
		}

	}

	private void receivedSecretReservationKeys(Channel channel, WisebedProtocol.SecretReservationKeys secretReservationKeys) {

		log.debug("ProtobufControllerServerHandler.receivedSecretReservationKeys()");

		authenticated =
				secretReservationKeys.getKeys(0).getUrnPrefix().equals("urn:wisebed:uzl1:") &&
				secretReservationKeys.getKeys(0).getKey().equals("abcd1234");

		if (authenticated) {
			log.debug("Client authenticated correctly.");
			schedules.put(channel, executor.scheduleAtFixedRate(new DataProducerRunnable(channel), 5, 5, TimeUnit.SECONDS));
		} else {
			log.debug("Wrong authentication credentials. Closing channel to client.");
			channel.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		log.warn("Unexpected exception from downstream: {}", e);
		e.getChannel().close();
	}

	private class DataProducerRunnable implements Runnable {

		private Channel channel;

		public DataProducerRunnable(Channel channel) {
			this.channel = channel;
		}

		@Override
		public void run() {

			WisebedProtocol.Message.NodeText.Builder nodeTextBuilder = WisebedProtocol.Message.NodeText.newBuilder()
					.setSourceNodeUrn("urn:wisebed:uzl1:0x1234")
					.setLevel(WisebedProtocol.Message.Level.DEBUG)
					.setText("Hello testbed world!");
			WisebedProtocol.Message.Builder messageBuilder = WisebedProtocol.Message.newBuilder()
					.setType(WisebedProtocol.Message.Type.NODE_TEXT)
					.setNodeText(nodeTextBuilder);
			WisebedProtocol.Envelope envelope = WisebedProtocol.Envelope.newBuilder()
					.setBodyType(WisebedProtocol.Envelope.BodyType.MESSAGE)
					.setMessage(messageBuilder)
					.build();

			log.debug("Writing message to channel. Size: {}", envelope.getSerializedSize());
			channel.write(envelope);
		}
	}
}
