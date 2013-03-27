package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClient;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClientFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class GatewayEventBusImpl extends AbstractService implements GatewayEventBus {

	private static final Logger log = LoggerFactory.getLogger(GatewayEventBus.class);

	private final GatewayConfig config;

	private final GatewayScheduler gatewayScheduler;

	private final EventBus eventBus;

	private final NettyClientFactory nettyClientFactory;

	private final GatewayChannelHandler gatewayChannelHandler;

	private NettyClient nettyClient;

	private ScheduledFuture<?> connectSchedule;

	private final Lock connectScheduleLock = new ReentrantLock();

	private volatile boolean shuttingDown = false;

	@Inject
	GatewayEventBusImpl(final GatewayConfig config,
						final GatewayScheduler gatewayScheduler,
						final EventBus eventBus,
						final NettyClientFactory nettyClientFactory,
						final GatewayChannelHandler gatewayChannelHandler) {
		this.config = config;
		this.gatewayScheduler = gatewayScheduler;
		this.eventBus = eventBus;
		this.nettyClientFactory = nettyClientFactory;
		this.gatewayChannelHandler = gatewayChannelHandler;
	}

	@Override
	public void register(final Object object) {
		eventBus.register(object);
	}

	@Override
	public void unregister(final Object object) {
		eventBus.unregister(object);
	}

	@Override
	public void post(final Object event) {
		eventBus.post(event);
	}

	@Override
	protected void doStart() {

		log.trace("GatewayEventBusImpl.doStart()");

		try {
			try {
				gatewayScheduler.execute(tryToConnectToPortalRunnable);
			} catch (Exception e) {
				// automatically handled
			}
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private final Runnable tryToConnectToPortalRunnable = new Runnable() {
		@Override
		public void run() {
			tryToConnectToPortal();
		}
	};

	private void tryToConnectToPortal() {

		log.debug("Trying to connect to portal server...");

		final InetSocketAddress portalAddress = new InetSocketAddress(
				config.portalOverlayAddress.getHostText(),
				config.portalOverlayAddress.getPort()
		);

		nettyClient = nettyClientFactory.create(
				portalAddress,
				channelObserver,
				new ProtobufVarint32FrameDecoder(),
				new ProtobufDecoder(Message.getDefaultInstance()),
				new ProtobufVarint32LengthFieldPrepender(),
				new ProtobufEncoder(),
				gatewayChannelHandler
		);

		try {
			nettyClient.start().get();
			log.info("Successfully connected to portal server.");
		} catch (Exception e) {
			nettyClient = null;
		}
	}

	private ChannelHandler channelObserver = new SimpleChannelUpstreamHandler() {
		@Override
		public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
			log.trace("GatewayEventBusImpl.channelConnected()");
			connectScheduleLock.lock();
			try {
				if (connectSchedule != null) {
					connectSchedule.cancel(true);
					connectSchedule = null;
				}
			} catch (Exception ex) {
				log.warn("Exception while cancelling reconnection schedule: {}", ex);
			} finally {
				connectScheduleLock.unlock();
			}
			super.channelConnected(ctx, e);
		}

		@Override
		public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
			log.trace("GatewayEventBusImpl.channelDisconnected()");

			if (!shuttingDown) {
				connectScheduleLock.lock();
				try {
					gatewayScheduler.execute(tryToConnectToPortalRunnable);
				} finally {
					connectScheduleLock.unlock();
				}
			}

			super.channelDisconnected(ctx, e);
		}

		@Override
		public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {

			@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
			final boolean couldNotConnect = e.getCause() instanceof ConnectException;

			if (couldNotConnect) {
				connectScheduleLock.lock();
				try {
					if (connectSchedule == null) {
						connectSchedule = gatewayScheduler.scheduleWithFixedDelay(
								tryToConnectToPortalRunnable, 10, 10, TimeUnit.SECONDS
						);
					}
				} finally {
					connectScheduleLock.unlock();
				}

			} else {
				super.exceptionCaught(ctx, e);
			}
		}
	};

	@Override
	protected void doStop() {

		log.trace("GatewayEventBusImpl.doStop()");

		shuttingDown = true;

		try {
			nettyClient.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
