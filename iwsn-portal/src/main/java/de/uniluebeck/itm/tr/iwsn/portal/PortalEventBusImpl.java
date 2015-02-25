package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.common.netty.ExceptionChannelHandler;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServer;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
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

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;

class PortalEventBusImpl extends AbstractService implements PortalEventBus {

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

    private final Logger log = LoggerFactory.getLogger(PortalEventBusImpl.class);

    private final PortalServerConfig config;

    private final SchedulerService schedulerService;

    private final EventBus eventBus;

    private final NettyServerFactory nettyServerFactory;

    private final PortalChannelHandler portalChannelHandler;

    private NettyServer nettyServer;

    @Inject
    public PortalEventBusImpl(final PortalServerConfig config,
                              final EventBusFactory eventBusFactory,
                              final NettyServerFactory nettyServerFactory,
                              final PortalChannelHandler portalChannelHandler,
                              final SchedulerService schedulerService) {
        this.config = config;
        this.schedulerService = schedulerService;
        this.eventBus = eventBusFactory.create("PortalEventBus");
        this.nettyServerFactory = nettyServerFactory;
        this.portalChannelHandler = portalChannelHandler;
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

    @Subscribe
    public void onRequest(final Request request) {
        eventBus.post(request);
    }

    @Override
    protected void doStart() {

        log.trace("PortalEventBusImpl.doStart()");

        try {

            portalChannelHandler.doStart();

            final ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    //noinspection unchecked
                    return Channels.pipeline(
                            new ExceptionChannelHandler(ClosedChannelException.class),
                            new IdleStateHandler(new HashedWheelTimer(), 30, 15, 0),
                            new ProtobufVarint32FrameDecoder(),
                            new ProtobufDecoder(Message.getDefaultInstance()),
                            new ProtobufVarint32LengthFieldPrepender(),
                            new ProtobufEncoder(),
                            KEEP_ALIVE_HANDLER,
                            //new KeepAliveHandler(schedulerService),
                            portalChannelHandler
                    );
                }
            };

            nettyServer = nettyServerFactory.create(new InetSocketAddress(config.getGatewayPort()), pipelineFactory);
            nettyServer.startAsync().awaitRunning();

            notifyStarted();

        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {

        log.trace("PortalEventBusImpl.doStop()");

        try {

            nettyServer.stopAsync().awaitTerminated();
            portalChannelHandler.doStop();

            notifyStopped();

        } catch (Exception e) {
            notifyFailed(e);
        }
    }
}
