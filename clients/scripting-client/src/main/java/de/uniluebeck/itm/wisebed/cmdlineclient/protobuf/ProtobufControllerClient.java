package de.uniluebeck.itm.wisebed.cmdlineclient.protobuf;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.tr.util.ListenerManagerImpl;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.api.sm.SecretReservationKey;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProtobufControllerClient extends ListenerManagerImpl<ProtobufController> {

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

	private ExecutorService bossExecutor;

	private ExecutorService workerExecutor;

	public static ProtobufControllerClient create(String hostname, int port,
												  List<SecretReservationKey> secretReservationKeys) {
		return new ProtobufControllerClient(hostname, port, secretReservationKeys);
	}

	private ProtobufControllerClient(String hostname, int port, List<SecretReservationKey> secretReservationKeys) {

		this.hostname = hostname;
		this.port = port;
		this.secretReservationKeys = secretReservationKeys;

		bossExecutor = Executors.newCachedThreadPool();
		workerExecutor = Executors.newCachedThreadPool();
	}

	public void connect() throws ConnectException {

		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(bossExecutor, workerExecutor));

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

		for (ProtobufController listener : listeners) {
			listener.onConnectionEstablished();
		}

		channel = channelFuture.getChannel();

		channel.getCloseFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				log.debug("Channel was closed.");
				for (ProtobufController listener : listeners) {
					listener.onConnectionClosed();
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						shutdown();
					}
				}
				).start();
			}
		}
		);

		channel = channelFuture.getChannel();
		log.debug("Connected.");

		WisebedProtocol.SecretReservationKeys.Builder secretReservationKeysBuilder =
				WisebedProtocol.SecretReservationKeys.newBuilder();
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
		if (!future.isSuccess()) {
			if (future.getCause() instanceof ConnectException) {
				throw (ConnectException) future.getCause();
			} else {
				throw new RuntimeException(future.getCause());
			}
		}

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				disconnect();
			}
		}
		)
		);

	}

	public void disconnect() {

		if (channel != null && channel.isOpen()) {
			channel.close().awaitUninterruptibly();
			channel = null;
		}

		shutdown();

	}

	private void shutdown() {

		if (bootstrap != null) {
			bootstrap.releaseExternalResources();
			bootstrap = null;
		}

	}

	void receivedRequestStatus(WisebedProtocol.RequestStatus requestStatus) {
		for (Controller listener : listeners) {
			listener.receiveStatus(Lists.newArrayList(convert(requestStatus)));
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
		switch (message.getType()) {
			case NODE_BINARY:
				for (Controller listener : listeners) {
					listener.receive(Lists.newArrayList(convert(message)));
				}
				break;
			case BACKEND:
				for (Controller listener : listeners) {
					listener.receiveNotification(Lists.<String>newArrayList(message.getBackend().getText()));
				}
				break;
		}

	}

	private Message convert(WisebedProtocol.Message message) {
		Message cMessage = new Message();
		cMessage.setTimestamp(datatypeFactory.newXMLGregorianCalendar(message.getTimestamp()));
		cMessage.setBinaryData(message.getNodeBinary().getData().toByteArray());
		cMessage.setSourceNodeId(message.getNodeBinary().getSourceNodeUrn());
		return cMessage;
	}

}
