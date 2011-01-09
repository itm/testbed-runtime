package de.uniluebeck.itm.tr.protobufcontroller;


import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ProtobufControllerServer {

	private static final Logger log = LoggerFactory.getLogger(ProtobufControllerServer.class);

	public static void main(String[] args) {

		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()
				)
		);

		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		bootstrap.setPipelineFactory(new ProtobufControllerServerPipelineFactory(scheduledExecutorService));

		int port = 8080;
		bootstrap.bind(new InetSocketAddress(port));
		log.debug("Bound ProtobufControllerServer to {}.", port);
	}
}
