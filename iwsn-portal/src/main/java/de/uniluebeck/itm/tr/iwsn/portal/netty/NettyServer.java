package de.uniluebeck.itm.tr.iwsn.portal.netty;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

import static org.jboss.netty.channel.Channels.pipeline;

public class NettyServer extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

	private final ChannelGroup allChannels;

	private final SocketAddress address;

	private final ChannelFactory factory;

	private final ServerBootstrap bootstrap;

	private final ChannelHandler[] handlers;

	@Inject
	NettyServer(final ChannelFactory factory,
				final ChannelGroup allChannels,
				@Assisted final SocketAddress address,
				@Assisted final Provider<ChannelHandler[]> handlers) {

		this.factory = factory;
		this.allChannels = allChannels;
		this.address = address;
		this.handlers = handlers.get();

		this.bootstrap = new ServerBootstrap(factory);
	}

	private ChannelPipelineFactory pipelineFactory(final ChannelHandler[] handlers) {
		return new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				final ChannelPipeline pipeline = pipeline(handlers);
				pipeline.addFirst("NettyServer.connectionsHandler", connectionsHandler);
				return pipeline;
			}
		};
	}

	private ChannelUpstreamHandler connectionsHandler = new ChannelUpstreamHandler() {
		@Override
		public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent e) throws Exception {
			if (e instanceof UpstreamChannelStateEvent) {
				final ChannelState state = ((UpstreamChannelStateEvent) e).getState();
				if (state == ChannelState.CONNECTED) {
					if (((UpstreamChannelStateEvent) e).getValue() != null) {
						allChannels.add(e.getChannel());
					} else {
						allChannels.remove(e.getChannel());
					}
				}
			}
		}
	};

	@Override
	protected void doStart() {

		log.trace("NettyServer.doStart(address={}, handlers={})", address, handlers);

		try {

			bootstrap.setPipelineFactory(pipelineFactory(handlers));
			bootstrap.setOption("child.tcpNoDelay", true);
			bootstrap.setOption("child.keepAlive", true);

			Channel channel = bootstrap.bind(address);
			allChannels.add(channel);
			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("NettyServer.doStop(address={}, handlers={})", address, handlers);

		try {

			allChannels.close().awaitUninterruptibly();
			factory.releaseExternalResources();
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
