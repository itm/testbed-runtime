package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;


import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.uniluebeck.itm.tr.util.Service;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceImpl;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.ProtobufInterface;

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
		bootstrap.setPipelineFactory(new ProtobufControllerServerPipelineFactory(this, sessionManagement));
		bootstrap.bind(new InetSocketAddress(
				config.getHostname() != null ? config.getHostname() : config.getIp(),
				config.getPort()
		));

	}

	@Override
	public void stop() {
		stopHandlers(null);
		bootstrap.releaseExternalResources();
	}

	private final List<ProtobufControllerServerHandler> handlers = Lists.newLinkedList();

	private final Lock handlersLock = new ReentrantLock();

	public void addHandler(ProtobufControllerServerHandler handler) {
		try {
			handlersLock.lock();
			handlers.add(handler);
		} finally {
			handlersLock.unlock();
		}
	}

	public void removeHandler(ProtobufControllerServerHandler handler) {
		try {
			handlersLock.lock();
			handlers.remove(handler);
		} finally {
			handlersLock.unlock();
		}
	}

	/**
	 * Stops a handler that handles {@code secretReservationKey} or all if {@code secretReservationKey} is null.
	 *
	 * @param secretReservationKey the secret reservation key identifying the reservation and thereby the handler
	 */
	public void stopHandlers(String secretReservationKey) {
		try {
			handlersLock.lock();
			for (ProtobufControllerServerHandler handler : Lists.newArrayList(handlers)) {
				if (null == secretReservationKey || handler.getSecretReservationKey().equals(secretReservationKey)) {
					handler.stop();
				}
			}
		} finally {
			handlersLock.unlock();
		}
	}
}
