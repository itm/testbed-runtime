package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;


import de.uniluebeck.itm.gtr.common.Service;
import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceImpl;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.ProtobufInterface;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ProtobufControllerServer implements Service {

	private static final Logger log = LoggerFactory.getLogger(ProtobufControllerServer.class);

	private SessionManagementServiceImpl sessionManagement;

	private ProtobufInterface config;

	private ServerBootstrap bootstrap;

	public ProtobufControllerServer(SessionManagementServiceImpl sessionManagement, ProtobufInterface config) {
		this.sessionManagement = sessionManagement;
		this.config = config;
	}

	@Override
	public void start() throws Exception {

		if (log.isInfoEnabled()) {
			log.info("Starting protocol buffer interface on {}:{}.",
					config.getHostname() != null ?
							config.getHostname() :
							config.getIp(),
					config.getPort()
			);
		}

		bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()
				)
		);
		bootstrap.setPipelineFactory(new ProtobufControllerServerPipelineFactory(sessionManagement));
		bootstrap.bind(new InetSocketAddress(
				config.getHostname() != null ? config.getHostname() : config.getIp(),
				config.getPort()
		));

	}

	@Override
	public void stop() {
		bootstrap.releaseExternalResources();
	}
}
