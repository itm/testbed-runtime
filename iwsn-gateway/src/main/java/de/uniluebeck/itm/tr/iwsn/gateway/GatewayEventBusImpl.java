package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.common.netty.ExceptionChannelHandler;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClient;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClientFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class GatewayEventBusImpl extends AbstractService implements GatewayEventBus {

    private static final Logger log = LoggerFactory.getLogger(GatewayEventBus.class);

    public static final IdleStateAwareChannelHandler KEEP_ALIVE_HANDLER = new IdleStateAwareChannelHandler() {
        @Override
        public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
            if (e.getState() == IdleState.READER_IDLE) {
                e.getChannel().close();
            } else if (e.getState() == IdleState.WRITER_IDLE) {
                Message keepAlive = Message
                        .newBuilder()
                        .setType(Message.Type.KEEP_ALIVE_ACK)
                        .build();
                e.getChannel().write(keepAlive);
            }
        }
    };

    private final GatewayConfig config;

    private final SchedulerService schedulerService;

    private final EventBus eventBus;

    private final NettyClientFactory nettyClientFactory;

    private final GatewayChannelHandler gatewayChannelHandler;

    private final Lock lock = new ReentrantLock();

    private boolean connected = false;

    private boolean shuttingDown = false;

    private NettyClient nettyClient;

    private ScheduledFuture<?> connectSchedule;

    private final Runnable connectToPortalRunnable = new Runnable() {
        @Override
        public void run() {

            lock.lock();
            try {
                if (connected) {
                    return;
                }
            } finally {
                lock.unlock();
            }

            log.info("Trying to connect to portal server...");

            final InetSocketAddress portalAddress = new InetSocketAddress(
                    config.getPortalAddress().getHostText(),
                    config.getPortalAddress().getPort()
            );

            final ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    //noinspection unchecked
                    return Channels.pipeline(
                            new ExceptionChannelHandler(ConnectException.class, ClosedChannelException.class),
                            new IdleStateHandler(new HashedWheelTimer(), 30, 15, 0),
                            channelObserver,
                            new ProtobufVarint32FrameDecoder(),
                            new ProtobufDecoder(Message.getDefaultInstance()),
                            new ProtobufVarint32LengthFieldPrepender(),
                            new ProtobufEncoder(),
                            KEEP_ALIVE_HANDLER,
                            //new KeepAliveHandler(schedulerService),
                            gatewayChannelHandler
                    );
                }
            };

            try {

                nettyClient = nettyClientFactory.create(portalAddress, pipelineFactory);
                nettyClient.startAsync().awaitRunning(5, TimeUnit.SECONDS);

            } catch (Exception e) {
                if (e.getCause() instanceof ConnectException) {
                    log.info("Failed to connect to portal server, trying again in {} seconds. Reason: {}", RECONNECT_DELAY_SECONDS, e.getCause().getMessage());
                }
                nettyClient = null;
            }
        }
    };
    private int RECONNECT_DELAY_SECONDS;

    @Inject
    GatewayEventBusImpl(final GatewayConfig config,
                        final SchedulerService schedulerService,
                        final EventBus eventBus,
                        final NettyClientFactory nettyClientFactory,
                        final GatewayChannelHandler gatewayChannelHandler) {
        this.config = config;
        this.schedulerService = schedulerService;
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
            lock.lock();
            try {
                RECONNECT_DELAY_SECONDS = 30;
                connectSchedule = schedulerService.scheduleWithFixedDelay(
                        connectToPortalRunnable,
                        1, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS
                );
            } finally {
                lock.unlock();
            }
            notifyStarted();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    private ChannelHandler channelObserver = new SimpleChannelUpstreamHandler() {

        @Override
        public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

            log.trace("GatewayEventBusImpl.channelConnected()");

            lock.lock();
            try {
                connected = true;
            } finally {
                lock.unlock();
            }

            log.info("Successfully connected to portal server");

            super.channelConnected(ctx, e);
        }

        @Override
        public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

            log.info("Lost connection to portal server");
            lock.lock();
            try {

                connected = false;
                if (!shuttingDown) {
                    schedulerService.execute(connectToPortalRunnable);
                }

            } finally {
                lock.unlock();
            }

            super.channelDisconnected(ctx, e);
        }
    };

    @Override
    protected void doStop() {

        log.trace("GatewayEventBusImpl.doStop()");

        lock.lock();
        try {

            shuttingDown = true;

            if (connectSchedule != null) {
                connectSchedule.cancel(true);
                connectSchedule = null;
            }

        } finally {
            lock.unlock();
        }

        try {

            if (nettyClient != null && nettyClient.isRunning()) {
                nettyClient.stopAsync().awaitTerminated();
            }
            notifyStopped();

        } catch (Exception e) {
            notifyFailed(e);
        }
    }
}
