package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.common.KeepAliveHandler;
import de.uniluebeck.itm.tr.iwsn.common.MessageUnwrapper;
import de.uniluebeck.itm.tr.iwsn.common.MessageWrapper;
import de.uniluebeck.itm.tr.iwsn.common.netty.ExceptionChannelHandler;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServer;
import de.uniluebeck.itm.tr.iwsn.portal.netty.NettyServerFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;

class ExternalPluginServiceImpl extends AbstractService implements ExternalPluginService {

	private final ExternalPluginServiceConfig config;

	private final NettyServerFactory nettyServerFactory;

	private final ExternalPluginServiceChannelHandler channelHandler;

	private final PortalEventBus portalEventBus;

	private NettyServer nettyServer;

	@Inject
	public ExternalPluginServiceImpl(final ExternalPluginServiceConfig config,
									 final NettyServerFactory nettyServerFactory,
									 final ExternalPluginServiceChannelHandler channelHandler,
									 final PortalEventBus portalEventBus) {
		this.config = config;
		this.nettyServerFactory = nettyServerFactory;
		this.channelHandler = channelHandler;
		this.portalEventBus = portalEventBus;
	}

	@Override
	protected void doStart() {
		try {

			if (config.getExternalPluginServicePort() != null) {

				ChannelPipelineFactory pipelineFactory = () -> {
					//noinspection unchecked
					return Channels.pipeline(
							new ExceptionChannelHandler(ClosedChannelException.class),
							new IdleStateHandler(new HashedWheelTimer(), 30, 15, 0),
							new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
							new ProtobufDecoder(Message.getDefaultInstance()),
							new LengthFieldPrepender(4, false),
							new ProtobufEncoder(),
							new KeepAliveHandler(),
							new MessageWrapper(),
							new MessageUnwrapper(),
							channelHandler
					);
				};

				InetSocketAddress address = new InetSocketAddress(config.getExternalPluginServicePort());

				nettyServer = nettyServerFactory.create(address, pipelineFactory);
				nettyServer.startAsync().awaitRunning();
			}

			portalEventBus.register(this);

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {

			portalEventBus.unregister(this);

			if (nettyServer != null && nettyServer.isRunning()) {
				nettyServer.stopAsync().awaitTerminated();
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}