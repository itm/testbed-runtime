package de.uniluebeck.itm.wisebed.cmdlineclient.protobuf;

import de.uniluebeck.itm.tr.util.AbstractListenable;
import eu.wisebed.testbed.api.wsn.v211.*;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class ProtobufControllerClient extends AbstractListenable<Controller> {

	private static final Logger log = LoggerFactory.getLogger(ProtobufControllerClient.class);

	private static DatatypeFactory datatypeFactory;

	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			log.error("" + e, e);
		}
	}

	private String hostname;

	private int port;

	private List<SecretReservationKey> secretReservationKeys;

	private Channel channel;

	private ClientBootstrap bootstrap;

	public static ProtobufControllerClient create(String hostname, int port, List<SecretReservationKey> secretReservationKeys) {
		return new ProtobufControllerClient(hostname, port, secretReservationKeys);
	}

	private ProtobufControllerClient(String hostname, int port, List<SecretReservationKey> secretReservationKeys) {
		this.hostname = hostname;
		this.port = port;
		this.secretReservationKeys = secretReservationKeys;
	}

	public void connect() {

		bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()
				)
		);

		// Configure the event pipeline factory.
		bootstrap.setPipelineFactory(new ProtobufControllerClientPipelineFactory(this));

		log.debug("Connecting to {}:{}", hostname, port);
		// Make a new connection.
		ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(hostname, port));

		// Wait until the connection is made successfully.
		ChannelFuture channelFuture = connectFuture.awaitUninterruptibly();
		if (!channelFuture.isSuccess()) {
			throw new RuntimeException("Could not connect to " + hostname + ":" + port);
		}
		channel = channelFuture.getChannel();

		channel.getCloseFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				log.info("Channel was closed.");
			}
		});

		channel = channelFuture.getChannel();
		log.debug("Connected.");

		WisebedProtocol.SecretReservationKeys.Builder secretReservationKeysBuilder = WisebedProtocol.SecretReservationKeys.newBuilder();
		for (SecretReservationKey secretReservationKey : secretReservationKeys) {
			secretReservationKeysBuilder.addKeys(WisebedProtocol.SecretReservationKeys.SecretReservationKey.newBuilder()
					.setUrnPrefix(secretReservationKey.getUrnPrefix())
					.setKey(secretReservationKey.getSecretReservationKey())
			);
		}

		WisebedProtocol.Envelope envelope = WisebedProtocol.Envelope.newBuilder()
				.setBodyType(WisebedProtocol.Envelope.BodyType.SECRET_RESERVATION_KEYS)
				.setSecretReservationKeys(secretReservationKeysBuilder)
				.build();

		log.debug("Sending secret reservation keys.");
		ChannelFuture future = channel.write(envelope);
		future.awaitUninterruptibly();

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				disconnect();
			}
		}));

	}

	public void disconnect() {

		if (channel != null && channel.isOpen()) {
			channel.close().awaitUninterruptibly();
			channel = null;
		}

		if (bootstrap != null) {
			bootstrap.releaseExternalResources();
			bootstrap = null;
		}

	}

	void receivedRequestStatus(WisebedProtocol.RequestStatus requestStatus) {
		for (Controller listener : listeners) {
			listener.receiveStatus(convert(requestStatus));
		}
	}

	private RequestStatus convert(WisebedProtocol.RequestStatus requestStatus) {
		RequestStatus cRequestStatus = new RequestStatus();
		cRequestStatus.setRequestId(requestStatus.getRequestId());
		for (WisebedProtocol.RequestStatus.Status status : requestStatus.getStatusList()) {
			cRequestStatus.getStatus().add(convert(status));
		}
		return cRequestStatus;
	}

	private Status convert(WisebedProtocol.RequestStatus.Status status) {
		Status cStatus = new Status();
		cStatus.setMsg(status.getMessage());
		cStatus.setNodeId(status.getNodeUrn());
		cStatus.setValue(status.getValue());
		return cStatus;
	}

	void receivedMessage(WisebedProtocol.Message message) {
		for (Controller listener : listeners) {
			listener.receive(convert(message));
		}
	}

	private Message convert(WisebedProtocol.Message message) {
		Message cMessage = new Message();
		TextMessage textMessage;
		BinaryMessage binaryMessage;
		switch (message.getType()) {
			case NODE_BINARY:
				binaryMessage = new BinaryMessage();
				binaryMessage.setBinaryType((byte) (0xFF & message.getNodeBinary().getType()));
				binaryMessage.setBinaryData(message.getNodeBinary().getData().toByteArray());
				cMessage.setBinaryMessage(binaryMessage);
				cMessage.setSourceNodeId(message.getNodeBinary().getSourceNodeUrn());
				break;
			case NODE_TEXT:
				textMessage = new TextMessage();
				textMessage.setMessageLevel(MessageLevel.fromValue(message.getNodeText().getLevel().name()));
				textMessage.setMsg(message.getNodeText().getText());
				cMessage.setTextMessage(textMessage);
				cMessage.setSourceNodeId(message.getNodeText().getSourceNodeUrn());
				break;
			case BACKEND:
				textMessage = new TextMessage();
				textMessage.setMessageLevel(MessageLevel.fromValue(message.getBackend().getLevel().name()));
				textMessage.setMsg(message.getBackend().getText());
				cMessage.setTextMessage(textMessage);
				cMessage.setSourceNodeId("backend");
				break;
		}
		cMessage.setTimestamp(datatypeFactory.newXMLGregorianCalendar(message.getTimestamp()));
		return cMessage;
	}

}
