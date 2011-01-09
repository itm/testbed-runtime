package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ProtobufControllerClient {

	public static void main(String[] args) {

		String host = "localhost";
		int port = 8080;

		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		// Configure the event pipeline factory.
		bootstrap.setPipelineFactory(new ProtobufControllerClientPipelineFactory());
		// Make a new connection.
		ChannelFuture connectFuture =
				bootstrap.connect(new InetSocketAddress(host, port));
		// Wait until the connection is made successfully.
		Channel channel = connectFuture.awaitUninterruptibly().getChannel();

		WisebedProtocol.SecretReservationKeys.Builder secretReservationKeysBuilder = WisebedProtocol.SecretReservationKeys.newBuilder()
				.addKeys(WisebedProtocol.SecretReservationKeys.SecretReservationKey.newBuilder()
						.setUrnPrefix("urn:wisebed:uzl1:")
						.setKey("abcd1234")
				);

		WisebedProtocol.Envelope envelope = WisebedProtocol.Envelope.newBuilder()
				.setBodyType(WisebedProtocol.Envelope.BodyType.SECRET_RESERVATION_KEYS)
				.setSecretReservationKeys(secretReservationKeysBuilder)
				.build();

		ChannelFuture future = channel.write(envelope);
		future.awaitUninterruptibly();

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		boolean exit = false;
		while (!exit) {
			try {
				String line = reader.readLine();
				if ("exit".equals(line)) {
					exit = true;
				}
			} catch (IOException e) {
				// ignore
			}
		}

		// Close the connection.
		channel.close().awaitUninterruptibly();

		// Shut down all thread pools to exit.
		bootstrap.releaseExternalResources();

	}

}
