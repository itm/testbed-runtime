package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceImpl;
import de.uniluebeck.itm.tr.runtime.portalapp.WSNServiceHandle;
import de.uniluebeck.itm.tr.runtime.wsnapp.UnknownNodeUrnsException;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;

public class ProtobufControllerServerHandler extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(ProtobufControllerServerHandler.class.getName());

	private boolean firstMessage = true;

	private ProtobufControllerServer protobufControllerServer;

	private final SessionManagementServiceImpl sessionManagement;

	private Channel channel;

	private WSNServiceHandle wsnServiceHandle;

	private String secretReservationKey;

	public ProtobufControllerServerHandler(final ProtobufControllerServer protobufControllerServer,
										   final SessionManagementServiceImpl sessionManagement) {
		this.protobufControllerServer = protobufControllerServer;
		this.sessionManagement = sessionManagement;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		log.debug("client connected: {}", e);
		channel = e.getChannel();
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		log.debug("client disconnected: {}", e);
		if (wsnServiceHandle != null) {
			wsnServiceHandle.getProtobufControllerHelper().removeChannel(e.getChannel());
		}
		protobufControllerServer.removeHandler(this);
		channel = null;
		super.channelDisconnected(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		WisebedProtocol.Envelope envelope = (WisebedProtocol.Envelope) e.getMessage();
		switch (envelope.getBodyType()) {
			case SECRET_RESERVATION_KEYS:
				receivedSecretReservationKeys(e, envelope);
				break;
			case MESSAGE:
				receivedMessage(ctx, e, envelope);
				break;
			default:
				log.warn("Received message other than secret reservation keys which is not allowed.");
				e.getChannel().close();
				break;
		}

	}

	private void receivedMessage(final ChannelHandlerContext ctx, final MessageEvent e,
								 final WisebedProtocol.Envelope envelope) {

		log.debug("ProtobufControllerServerHandler.receivedMessage({}, {}, {})", new Object[]{ctx, e, envelope});

		if (firstMessage) {
			log.warn("Received message before receiving secret reservation keys. Closing channel: {}", channel);
			ctx.getChannel().close();
			return;
		}

		final WisebedProtocol.Message message = envelope.getMessage();


		final HashSet<String> nodeUrns = Sets.newHashSet(message.getNodeBinary().getDestinationNodeUrnsList());

		try {

			final byte[] bytes = message.getNodeBinary().getData().toByteArray();
			final String sourceNodeUrn = message.getNodeBinary().getSourceNodeUrn();
			final String timestamp = message.getTimestamp();

			if (log.isDebugEnabled()) {
				log.debug("Sending message {} to nodeUrns {}", toPrintableString(bytes, 200), nodeUrns);
			}

			final WSNApp.Callback callback = new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {
					if (requestStatus.getStatus().getValue() != 1) {
						String text = requestStatus.getStatus().getNodeId() + ": " +
								"\"" + requestStatus.getStatus().getMsg() + "\"" +
								" (value=" + requestStatus.getStatus().getValue() + ")";
						log.warn(text);
						sendBackendMessage(ctx, e.getRemoteAddress(), text);
					}
				}

				@Override
				public void failure(final Exception exception) {
					String text = "Message delivery to " + nodeUrns + " failed. Reason: " + exception
							.getMessage();
					log.error(text);
					sendBackendMessage(ctx, e.getRemoteAddress(), text);
				}
			};
			wsnServiceHandle.getWsnApp().send(nodeUrns, bytes, sourceNodeUrn, timestamp, callback);

		} catch (UnknownNodeUrnsException exception) {
			String text = "Message delivery to " + nodeUrns + " failed. Reason: " + exception.getMessage();
			log.error(text);
			sendBackendMessage(ctx, e.getRemoteAddress(), text);
		}


	}

	private void sendBackendMessage(final ChannelHandlerContext ctx, final SocketAddress remoteAddress,
									final String text) {

		WisebedProtocol.Message.Backend.Builder backendBuilder = WisebedProtocol.Message.Backend.newBuilder()
				.setText(text);
		WisebedProtocol.Message.Builder messageBuilder = WisebedProtocol.Message.newBuilder()
				.setType(WisebedProtocol.Message.Type.BACKEND)
				.setBackend(backendBuilder);
		WisebedProtocol.Envelope envelope = WisebedProtocol.Envelope.newBuilder()
				.setMessage(messageBuilder)
				.setBodyType(WisebedProtocol.Envelope.BodyType.MESSAGE)
				.build();
		DefaultChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), true);
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {
				if (future.isCancelled() && log.isWarnEnabled()) {
					log.warn("Delivery of backend message ({}) to {} was cancelled.", text, remoteAddress);
				} else if (future.isSuccess() && log.isTraceEnabled()) {
					log.trace("Delivery of backend message ({}) to {} successful.", text, remoteAddress);
				}
			}
		}
		);
		ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future, envelope, remoteAddress));

	}

	private void receivedSecretReservationKeys(final MessageEvent e, final WisebedProtocol.Envelope envelope) {

		if (firstMessage) {
			firstMessage = false;
		}

		checkArgument(!firstMessage, "Secret reservation keys are only allowed to be sent as the very first message.");
		checkArgument(envelope.hasSecretReservationKeys(), "Envelope is missing secret reservation keys");

		log.debug("ProtobufControllerServerHandler.receivedSecretReservationKeys()");

		secretReservationKey = envelope.getSecretReservationKeys().getKeys(0).getKey();
		wsnServiceHandle = sessionManagement.getWsnServiceHandle(secretReservationKey);

		if (wsnServiceHandle == null) {

			log.debug(
					"Received reservation key for either unknown or not yet started WSN instance. Trying to start instance..."
			);
			List<SecretReservationKey> secretReservationKeys = convert(envelope.getSecretReservationKeys());

			try {
				sessionManagement.getInstance(secretReservationKeys, "NONE");
				wsnServiceHandle = sessionManagement.getWsnServiceHandle(secretReservationKey);
			} catch (UnknownReservationIdException_Exception e1) {
				log.debug("{}", e1.getMessage());
				channel.close();
				return;
			} catch (Exception e1) {
				log.debug("" + e1, e1);
				channel.close();
				return;
			}

			if (wsnServiceHandle == null) {
				log.debug("Invalid secret reservation key. Closing channel.");
				channel.close();
				return;
			}

		}

		log.debug("Valid secret reservation key. Adding listener to WSN App instance.");
		wsnServiceHandle.getProtobufControllerHelper().addChannel(e.getChannel());
		protobufControllerServer.addHandler(this);

	}

	private List<SecretReservationKey> convert(WisebedProtocol.SecretReservationKeys secretReservationKeys) {
		List<SecretReservationKey> retKeys = Lists.newArrayList();
		for (WisebedProtocol.SecretReservationKeys.SecretReservationKey key : secretReservationKeys.getKeysList()) {
			retKeys.add(convert(key));
		}
		return retKeys;
	}

	private SecretReservationKey convert(WisebedProtocol.SecretReservationKeys.SecretReservationKey key) {
		SecretReservationKey retKey = new SecretReservationKey();
		retKey.setUrnPrefix(key.getUrnPrefix());
		retKey.setSecretReservationKey(key.getKey());
		return retKey;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		log.warn("Unexpected exception from downstream: {}. Stacktrace: {}",
				e.getCause().getMessage(),
				Throwables.getStackTraceAsString(e.getCause())
		);
		e.getChannel().close();
	}

	public void stop() {
		log.debug("Stopping ProtobufControllerHandler for channel {}...", channel);
		// if the channel object is null it has already been closed
		if (channel != null) {
			channel.close();
		}
	}

	public String getSecretReservationKey() {
		return secretReservationKey;
	}
}
